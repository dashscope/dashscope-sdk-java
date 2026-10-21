// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.resources.audio.RecognitionSession;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.types.audio.AsrResult;
import com.alibaba.dashscope.types.audio.AsrTranscribeParams;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code audio.asr} (audio design §11). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class AsrMockTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWebServer server;
  private DashScopeClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .wsBaseUrl(server.url("/api-ws/v1/inference").toString())
            .timeout(Duration.ofSeconds(5))
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  /** Server-side ws endpoint reacting to run/finish-task. */
  private static final class AsrServerSocket extends WebSocketListener {
    final CountDownLatch opened = new CountDownLatch(1);
    final CountDownLatch closed = new CountDownLatch(1);
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final BlockingQueue<JsonNode> texts = new LinkedBlockingQueue<>();
    final BlockingQueue<ByteString> binaries = new LinkedBlockingQueue<>();
    volatile boolean respondToFinish = true;

    @Override
    public void onOpen(WebSocket webSocket, okhttp3.Response response) {
      socket.set(webSocket);
      opened.countDown();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      try {
        JsonNode frame = MAPPER.readTree(text);
        texts.add(frame);
        String action = frame.get("header").get("action").asText();
        String taskId = frame.get("header").get("task_id").asText();
        if ("run-task".equals(action)) {
          send(taskId, "task-started", null);
        } else if ("finish-task".equals(action) && respondToFinish) {
          send(
              taskId,
              "task-finished",
              "{\"output\":{},\"usage\":{\"input_tokens\":1,\"output_tokens\":2,"
                  + "\"total_tokens\":3}}");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
      binaries.add(bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(code, reason);
      closed.countDown();
    }

    void awaitOpen() throws InterruptedException {
      assertTrue(opened.await(10, TimeUnit.SECONDS), "server socket never opened");
    }

    /** Sends a task frame; {@code payloadJson} is the payload OBJECT (or null for {@code {}}). */
    void send(String taskId, String event, String payloadJson) {
      String payload = payloadJson != null ? payloadJson : "{}";
      socket
          .get()
          .send(
              "{\"header\":{\"task_id\":\""
                  + taskId
                  + "\",\"event\":\""
                  + event
                  + "\"},\"payload\":"
                  + payload
                  + "}");
    }

    JsonNode nextText() throws InterruptedException {
      JsonNode frame = texts.poll(10, TimeUnit.SECONDS);
      assertNotNull(frame, "timed out waiting for a client text frame");
      return frame;
    }
  }

  private AsrServerSocket enqueueUpgrade() {
    AsrServerSocket listener = new AsrServerSocket();
    server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
    return listener;
  }

  private static AsrRecognizeParams params() {
    return AsrRecognizeParams.builder()
        .model("paraformer-realtime-v2")
        .format("pcm")
        .sampleRate(16000)
        .build();
  }

  @Test
  void recognizeFullLifecycle() throws Exception {
    AsrServerSocket socket = enqueueUpgrade();
    try (RecognitionSession asr = client.audio().asr().recognize(params())) {
      asr.start();
      socket.awaitOpen();

      JsonNode runTask = socket.nextText();
      assertEquals("run-task", runTask.get("header").get("action").asText());
      assertEquals("duplex", runTask.get("header").get("streaming").asText());
      JsonNode payload = runTask.get("payload");
      assertEquals("audio", payload.get("task_group").asText());
      assertEquals("asr", payload.get("task").asText());
      assertEquals("recognition", payload.get("function").asText());
      assertEquals("paraformer-realtime-v2", payload.get("model").asText());
      assertEquals("pcm", payload.get("parameters").get("format").asText());
      assertEquals(16000, payload.get("parameters").get("sample_rate").asInt());
      String taskId = runTask.get("header").get("task_id").asText();
      assertEquals(taskId, asr.getTaskId());

      asr.sendAudioFrame(new byte[] {1, 2, 3, 4});
      ByteString frame = socket.binaries.poll(10, TimeUnit.SECONDS);
      assertNotNull(frame);
      assertEquals(4, frame.size());

      asr.updateContext(Collections.singletonMap("context", "对话上下文"));
      JsonNode continueTask = socket.nextText();
      assertEquals("continue-task", continueTask.get("header").get("action").asText());
      assertEquals(taskId, continueTask.get("header").get("task_id").asText());
      assertEquals("对话上下文", continueTask.get("payload").get("input").get("context").asText());

      // Heartbeat sentence + a real sentence.
      socket.send(
          taskId,
          "result-generated",
          "{\"output\":{\"sentence\":{\"begin_time\":0,\"end_time\":100,"
              + "\"text\":\"\",\"heartbeat\":true}}}");
      socket.send(
          taskId,
          "result-generated",
          "{\"output\":{\"sentence\":{\"begin_time\":100,\"end_time\":900,"
              + "\"text\":\"你好\",\"sentence_end\":true}}}");

      asr.stop();
      JsonNode finishTask = socket.nextText();
      assertEquals("finish-task", finishTask.get("header").get("action").asText());

      List<AsrResult> results = new ArrayList<>();
      for (AsrResult r : asr.results()) {
        results.add(r);
      }
      // Heartbeat filtered; exactly one typed result.
      assertEquals(1, results.size());
      assertEquals("你好", results.get(0).getSentence().getText());
      assertEquals(Long.valueOf(100), results.get(0).getSentence().getBeginTime());
      assertTrue(results.get(0).getSentence().getSentenceEnd());

      assertNotNull(asr.getFirstPackageDelay());
      assertNotNull(asr.getLastPackageDelay());
      assertNotNull(asr.getUsage());
      assertEquals(3, asr.getUsage().getTotalTokens());
      assertNotNull(asr.getLastResponse());

      // events() replays the full raw stream including task-finished.
      List<String> events = new ArrayList<>();
      for (JsonNode raw : asr.events()) {
        events.add(raw.get("header").get("event").asText());
      }
      assertTrue(events.contains("result-generated"));
      assertEquals("task-finished", events.get(events.size() - 1));
    }
  }

  @Test
  void preTaskIdWinsOverRandomUuid() throws Exception {
    AsrServerSocket socket = enqueueUpgrade();
    try (RecognitionSession asr =
        client.audio().asr().recognize(
            AsrRecognizeParams.builder()
                .model("paraformer-realtime-v2")
                .format("pcm")
                .sampleRate(16000)
                .preTaskId("fixed-task-id")
                .build())) {
      asr.start();
      JsonNode runTask = socket.nextText();
      assertEquals("fixed-task-id", runTask.get("header").get("task_id").asText());
      assertEquals("fixed-task-id", runTask.get("payload").get("parameters").get("pre_task_id").asText());
      assertEquals("fixed-task-id", asr.getTaskId());
    }
  }

  @Test
  void phraseIdMapsToTopLevelResources() throws Exception {
    AsrServerSocket socket = enqueueUpgrade();
    try (RecognitionSession asr =
        client.audio().asr().recognize(
            AsrRecognizeParams.builder()
                .model("paraformer-realtime-v2")
                .phraseId("phrase-123")
                .build())) {
      asr.start();
      JsonNode runTask = socket.nextText();
      JsonNode resources = runTask.get("payload").get("resources");
      assertNotNull(resources);
      assertEquals("phrase-123", resources.get(0).get("resource_id").asText());
      assertEquals("asr_phrase", resources.get(0).get("resource_type").asText());
      assertFalse(runTask.get("payload").get("parameters").has("phrase_id"));
    }
  }

  @Test
  void taskFailedRaisesBusinessErrorAndNoFinishedEvent() throws Exception {
    AsrServerSocket socket = enqueueUpgrade();
    try (RecognitionSession asr = client.audio().asr().recognize(params())) {
      asr.start();
      socket.nextText();
      String taskId = asr.getTaskId();
      socket.send(
          taskId,
          "result-generated",
          "{\"output\":{\"sentence\":{\"text\":\"你\"}}}");
      socket.socket.get().send(
          "{\"header\":{\"task_id\":\"" + taskId + "\",\"event\":\"task-failed\","
              + "\"error_code\":\"DecodingError\",\"error_message\":\"decode failed\"},"
              + "\"payload\":{}}");

      List<String> texts = new ArrayList<>();
      ApiStatusException e =
          assertThrows(
              ApiStatusException.class,
              () -> {
                for (AsrResult r : asr.results()) {
                  texts.add(r.getSentence().getText());
                }
              });
      assertEquals("DecodingError", e.getCode());
      assertEquals("decode failed", e.getMessage());
      assertEquals(200, e.getStatusCode());
      assertEquals(java.util.Arrays.asList("你"), texts);
      // §6 close 时序: the socket is closed before the failure reaches the caller.
      assertTrue(socket.closed.await(10, TimeUnit.SECONDS), "socket not closed after task-failed");
      // No task-finished was ever produced: the failure is the terminal truth.
      List<String> events = new ArrayList<>();
      assertThrows(
          ApiStatusException.class,
          () -> {
            for (JsonNode raw : asr.events()) {
              events.add(raw.get("header").get("event").asText());
            }
          });
      assertFalse(events.contains("task-finished"));
    }
  }

  @Test
  void stopWithoutTaskFinishedTimesOut() throws Exception {
    AsrServerSocket socket = enqueueUpgrade();
    socket.respondToFinish = false;
    try (RecognitionSession asr = client.audio().asr().recognize(params())) {
      asr.start();
      socket.nextText();
      assertThrows(ApiTimeoutException.class, () -> asr.stop(Duration.ofMillis(300)));
    }
  }

  @Test
  void closeIsIdempotentAndSessionRestarts() throws Exception {
    AsrServerSocket first = enqueueUpgrade();
    AsrServerSocket second = enqueueUpgrade();
    RecognitionSession asr = client.audio().asr().recognize(params());
    asr.start();
    first.nextText();
    asr.close();
    asr.close();
    // Restart: new task, new socket.
    asr.start();
    JsonNode runTask = second.nextText();
    assertEquals("run-task", runTask.get("header").get("action").asText());
    asr.close();
  }

  @Test
  void sharedConnectionSurvivesSessionClose() throws Exception {
    AsrServerSocket socket = enqueueUpgrade();
    WsConnection connection = client.wsConnection();
    socket.awaitOpen();
    RecognitionSession asr = client.audio().asr().recognize(params(), connection);
    asr.start();
    socket.nextText();
    asr.close();
    // The shared socket stays usable after the session detaches.
    com.alibaba.dashscope.transport.ws.TaskChannel other = connection.attach("task-other");
    assertNotNull(other);
    connection.close();
  }

  @Test
  void transcribeSubmitsAsyncJobAndWaits() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"task_id\":\"tx-1\",\"task_status\":\"PENDING\"},"
                  + "\"request_id\":\"r-submit\"}"));
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"task_id\":\"tx-1\",\"task_status\":\"SUCCEEDED\","
                  + "\"results\":[{\"transcription_url\":\"http://x/1.json\"}]},"
                  + "\"usage\":{\"input_tokens\":10},\"request_id\":\"r-fetch\"}"));

    Job job =
        client.audio().asr().transcribe(
            AsrTranscribeParams.builder()
                .fileUrls(java.util.Arrays.asList("http://files/a.wav"))
                .build());

    assertEquals("tx-1", job.getTaskId());
    assertTrue(job.isSucceeded());

    RecordedRequest submit = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("enable", submit.getHeader("X-DashScope-Async"));
    assertEquals("/services/audio/asr/transcription", submit.getPath());
    JsonNode body = MAPPER.readTree(submit.getBody().readUtf8());
    assertEquals(AsrTranscribeParams.DEFAULT_MODEL, body.get("model").asText());
    assertEquals("http://files/a.wav", body.get("input").get("file_urls").get(0).asText());

    RecordedRequest fetch = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", fetch.getMethod());
    assertEquals("/tasks/tx-1", fetch.getPath());
  }
}
