// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.errors.ApiStatusException;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.resources.tingwu.TingWuRealtimeSession;
import com.alibaba.dashscope.v3.types.tingwu.TingWuJobParams;
import com.alibaba.dashscope.v3.types.tingwu.TingWuRealtimeParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

/** MockWebServer tests for {@code tingwu} (jobs + realtime; design §3 resource tree). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class TingWuMockTest {
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

  /** Server-side ws endpoint reacting to run/finish-task (tingwu flavor). */
  private static final class TingWuServerSocket extends WebSocketListener {
    final CountDownLatch opened = new CountDownLatch(1);
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final BlockingQueue<JsonNode> texts = new LinkedBlockingQueue<>();
    final BlockingQueue<ByteString> binaries = new LinkedBlockingQueue<>();

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
        } else if ("finish-task".equals(action)) {
          send(taskId, "result-generated", "{\"output\":{\"action\":\"speech-end\"}}");
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

  private TingWuServerSocket enqueueUpgrade() {
    TingWuServerSocket listener = new TingWuServerSocket();
    server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
    return listener;
  }

  private static TingWuRealtimeParams realtimeParams() {
    return TingWuRealtimeParams.builder().model("tingwu-turbo").appId("app-1").build();
  }

  @Test
  void jobsCreateSubmitsAsyncTaskAndWaits() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"task_id\":\"tw-1\",\"task_status\":\"PENDING\"},"
                  + "\"request_id\":\"r-submit\"}"));
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"task_id\":\"tw-1\",\"task_status\":\"SUCCEEDED\","
                  + "\"TaskId\":\"tw-1\",\"Result\":\"{\\\"summary\\\":\\\"ok\\\"}\"},"
                  + "\"request_id\":\"r-fetch\"}"));

    Map<String, Object> input = new LinkedHashMap<>();
    input.put("source", Collections.singletonMap("url", "http://files/meeting.mp3"));
    Job created =
        client.tingwu().jobs().create(
            TingWuJobParams.builder()
                .model("tingwu-turbo")
                .input(input)
                .parameters(Collections.singletonMap("maxWords", 200))
                .build());
    assertEquals("tw-1", created.getTaskId());

    Job done = client.tingwu().jobs().wait(created.getTaskId(), null);
    assertTrue(done.isSucceeded());

    RecordedRequest submit = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", submit.getMethod());
    assertEquals("/services/aigc/multimodal-generation/generation", submit.getPath());
    assertEquals("enable", submit.getHeader("X-DashScope-Async"));
    JsonNode body = MAPPER.readTree(submit.getBody().readUtf8());
    assertEquals("tingwu-turbo", body.get("model").asText());
    assertEquals(
        "http://files/meeting.mp3", body.get("input").get("source").get("url").asText());
    assertEquals(200, body.get("parameters").get("maxWords").asInt());

    RecordedRequest fetch = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", fetch.getMethod());
    assertEquals("/tasks/tw-1", fetch.getPath());
  }

  @Test
  void jobsParametersOmittedWhenEmpty() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"task_id\":\"tw-2\",\"task_status\":\"PENDING\"},"
                  + "\"request_id\":\"r2\"}"));

    client.tingwu().jobs().create(
        TingWuJobParams.builder()
            .model("tingwu-turbo")
            .input(Collections.singletonMap("key", "v"))
            .build());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
    assertFalse(body.has("parameters"));
    assertEquals("v", body.get("input").get("key").asText());
  }

  @Test
  void jobsValidationRejectsBadInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> TingWuJobParams.builder().input(Collections.singletonMap("k", "v")).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> TingWuJobParams.builder().model("tingwu-turbo").build());
    assertEquals(0, server.getRequestCount());
  }

  @Test
  void realtimeLifecycle() throws Exception {
    TingWuServerSocket socket = enqueueUpgrade();
    try (TingWuRealtimeSession session =
        client.tingwu().realtime(
            TingWuRealtimeParams.builder()
                .model("tingwu-turbo")
                .appId("app-1")
                .format("pcm")
                .sampleRate(16000)
                .terminology("term-1")
                .maxEndSilence(800)
                .workspaceId("ws-1")
                .dataId("data-1")
                .build())) {
      session.start();
      socket.awaitOpen();

      JsonNode runTask = socket.nextText();
      assertEquals("run-task", runTask.get("header").get("action").asText());
      assertEquals("duplex", runTask.get("header").get("streaming").asText());
      JsonNode payload = runTask.get("payload");
      assertEquals("aigc", payload.get("task_group").asText());
      assertEquals("multimodal-generation", payload.get("task").asText());
      assertEquals("generation", payload.get("function").asText());
      assertEquals("tingwu-turbo", payload.get("model").asText());
      JsonNode input = payload.get("input");
      assertEquals("app-1", input.get("appId").asText());
      assertEquals("start", input.get("directive").asText());
      assertEquals("ws-1", input.get("workspace_id").asText());
      assertEquals("data-1", input.get("dataId").asText());
      JsonNode parameters = payload.get("parameters");
      assertEquals("pcm", parameters.get("format").asText());
      assertEquals(16000, parameters.get("sampleRate").asInt());
      assertEquals("term-1", parameters.get("terminology").asText());
      assertEquals(800, parameters.get("maxEndSilence").asInt());
      assertEquals(runTask.get("header").get("task_id").asText(), session.getTaskId());

      session.sendAudioFrame(new byte[] {1, 2, 3});
      ByteString frame = socket.binaries.poll(10, TimeUnit.SECONDS);
      assertNotNull(frame);
      assertEquals(3, frame.size());

      // One interim event before stop; the server answers finish-task with speech-end.
      socket.send(
          session.getTaskId(),
          "result-generated",
          "{\"output\":{\"action\":\"transcription\",\"text\":\"你好\"}}");
      session.stop();
      JsonNode finishTask = socket.nextText();
      assertEquals("finish-task", finishTask.get("header").get("action").asText());
      JsonNode finishInput = finishTask.get("payload").get("input");
      assertEquals("app-1", finishInput.get("appId").asText());
      assertEquals("stop", finishInput.get("directive").asText());

      List<JsonNode> events = new ArrayList<>();
      for (JsonNode raw : session.events()) {
        events.add(raw);
      }
      assertEquals(2, events.size());
      assertEquals("transcription", events.get(0).get("payload").get("output").get("action").asText());
      assertEquals("speech-end", events.get(1).get("payload").get("output").get("action").asText());
    }
  }

  @Test
  void realtimeDefaultsAndNamedWinOverParameters() throws Exception {
    TingWuServerSocket socket = enqueueUpgrade();
    Map<String, Object> hatch = new LinkedHashMap<>();
    hatch.put("terminology", "hatch-term");
    hatch.put("customKnob", true);
    try (TingWuRealtimeSession session =
        client.tingwu().realtime(
            TingWuRealtimeParams.builder()
                .model("tingwu-turbo")
                .appId("app-1")
                .terminology("named-term")
                .parameters(hatch)
                .build())) {
      session.start();
      JsonNode runTask = socket.nextText();
      JsonNode parameters = runTask.get("payload").get("parameters");
      // Defaults pcm/16000; named terminology wins over the hatch; unknown knobs pass through.
      assertEquals("pcm", parameters.get("format").asText());
      assertEquals(16000, parameters.get("sampleRate").asInt());
      assertEquals("named-term", parameters.get("terminology").asText());
      assertTrue(parameters.get("customKnob").asBoolean());
      JsonNode input = runTask.get("payload").get("input");
      assertFalse(input.has("workspace_id"));
      assertFalse(input.has("dataId"));
    }
  }

  @Test
  void realtimePayloadTaskFailedRaises() throws Exception {
    TingWuServerSocket socket = enqueueUpgrade();
    try (TingWuRealtimeSession session = client.tingwu().realtime(realtimeParams())) {
      session.start();
      socket.nextText();
      String taskId = session.getTaskId();
      socket
          .socket
          .get()
          .send(
              "{\"header\":{\"task_id\":\""
                  + taskId
                  + "\",\"event\":\"result-generated\",\"request_id\":\"rid-9\"},"
                  + "\"payload\":{\"output\":{\"action\":\"task-failed\","
                  + "\"errorCode\":\"TaskFailed\",\"errorMessage\":\"asr broke\"}}}");

      ApiStatusException e =
          assertThrows(
              ApiStatusException.class,
              () -> {
                for (JsonNode ignored : session.events()) {
                  // no frame survives the failure
                }
              });
      assertEquals("TaskFailed", e.getCode());
      assertEquals("asr broke", e.getMessage());
      assertEquals("rid-9", e.getRequestId());
    }
  }

  @Test
  void asyncJobsCreateCompletes() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"task_id\":\"tw-3\",\"task_status\":\"PENDING\"},"
                  + "\"request_id\":\"r3\"}"));
    try (DashScopeAsyncClient asyncClient =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      Job job =
          asyncClient
              .tingwu()
              .jobs()
              .create(
                  TingWuJobParams.builder()
                      .model("tingwu-turbo")
                      .input(Collections.singletonMap("k", "v"))
                      .build())
              .get(10, TimeUnit.SECONDS);
      assertEquals("tw-3", job.getTaskId());
    }
  }
}
