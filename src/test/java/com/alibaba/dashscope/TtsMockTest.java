// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.resources.audio.SynthesisSession;
import com.alibaba.dashscope.types.audio.TtsConnectParams;
import com.alibaba.dashscope.types.audio.TtsSynthesizeParams;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
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

/** MockWebServer tests for {@code audio.tts} (audio design §11). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class TtsMockTest {
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

  /** Server-side duplex TTS endpoint: started on run-task; audio+finished on finish-task. */
  private static final class TtsServerSocket extends WebSocketListener {
    final CountDownLatch opened = new CountDownLatch(1);
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final BlockingQueue<JsonNode> texts = new LinkedBlockingQueue<>();
    final List<byte[]> audioToSend = new ArrayList<>();

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
          for (byte[] audio : audioToSend) {
            webSocket.send(ByteString.of(audio));
          }
          send(
              taskId,
              "task-finished",
              "{\"output\":{},\"usage\":{\"input_tokens\":3,\"output_tokens\":0,\"total_tokens\":3}}");
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(code, reason);
    }

    void awaitOpen() throws InterruptedException {
      assertTrue(opened.await(10, TimeUnit.SECONDS), "server socket never opened");
    }

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

  private TtsServerSocket enqueueUpgrade() {
    TtsServerSocket listener = new TtsServerSocket();
    server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
    return listener;
  }

  private static MockResponse sse(String body) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body);
  }

  @Test
  void httpSynthesizeAggregatesAudioChunks() throws Exception {
    byte[] a1 = {1, 2, 3};
    byte[] a2 = {4, 5};
    server.enqueue(
        sse(
            "data: {\"output\":{\"audio\":\""
                + Base64.getEncoder().encodeToString(a1)
                + "\"}}\n\n"
                + "data: {\"output\":{\"audio\":\""
                + Base64.getEncoder().encodeToString(a2)
                + "\"},\"usage\":{\"input_tokens\":2}}\n\n"
                + "data: [DONE]\n\n"));

    byte[] audio =
        client
            .audio()
            .tts()
            .synthesize(
                TtsSynthesizeParams.builder()
                    .model("cosyvoice-v3-flash")
                    .text("你好")
                    .voice("longanyang")
                    .build());

    assertEquals(5, audio.length);
    assertEquals(1, audio[0]);
    assertEquals(5, audio[4]);

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/audio/tts/SpeechSynthesizer", request.getPath());
    assertEquals("enable", request.getHeader("X-DashScope-SSE"));
    JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
    assertEquals("cosyvoice-v3-flash", body.get("model").asText());
    // voice/format live INSIDE input (2.x parity).
    assertEquals("你好", body.get("input").get("text").asText());
    assertEquals("longanyang", body.get("input").get("voice").asText());
  }

  @Test
  void qwenTtsRoutesToMultimodalGenerationAndReadsAudioDataObject() throws Exception {
    byte[] a1 = {9, 9};
    server.enqueue(
        sse(
            "data: {\"output\":{\"audio\":{\"data\":\""
                + Base64.getEncoder().encodeToString(a1)
                + "\",\"id\":\"aud-1\"}}}\n\n"
                + "data: {\"usage\":{\"input_tokens\":1}}\n\n"
                + "data: [DONE]\n\n"));

    List<byte[]> chunks = new ArrayList<>();
    try (ResponseStream<byte[]> stream =
        client.audio().tts().stream(
            TtsSynthesizeParams.builder().model("qwen3-tts-flash").text("hi").build())) {
      for (byte[] chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(1, chunks.size());
    assertEquals(2, chunks.get(0).length);
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/multimodal-generation/generation", request.getPath());
  }

  @Test
  void httpPathRoutingFollowsModelPrefix() throws Exception {
    // Pins the remaining AudioModelRouter table rows (audio design §8); cosyvoice-v3-flash and
    // qwen3-tts-flash are covered by the two tests above.
    String[][] routes = {
      {"qwen-audio-tts", "/services/audio/tts/SpeechSynthesizer"},
      {"qwen-tts", "/services/aigc/multimodal-generation/generation"},
      {"sambert-v1", "/services/audio/tts/SpeechSynthesizer"},
    };
    for (String[] route : routes) {
      server.enqueue(sse("data: [DONE]\n\n"));
      client
          .audio()
          .tts()
          .synthesize(TtsSynthesizeParams.builder().model(route[0]).text("hi").voice("v").build());
      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals(route[1], request.getPath(), route[0]);
    }
  }

  @Test
  void duplexSessionLifecycle() throws Exception {
    TtsServerSocket socket = enqueueUpgrade();
    socket.audioToSend.add(new byte[] {7, 7});
    socket.audioToSend.add(new byte[] {8});

    try (SynthesisSession tts =
        client
            .audio()
            .tts()
            .connect(
                TtsConnectParams.builder()
                    .model("cosyvoice-v2")
                    .voice("longxiaochun")
                    .format("mp3")
                    .build())) {
      tts.start();
      socket.awaitOpen();

      JsonNode runTask = socket.nextText();
      assertEquals("run-task", runTask.get("header").get("action").asText());
      JsonNode payload = runTask.get("payload");
      assertEquals("audio", payload.get("task_group").asText());
      assertEquals("tts", payload.get("task").asText());
      assertEquals("SpeechSynthesizer", payload.get("function").asText());
      assertEquals("cosyvoice-v2", payload.get("model").asText());
      assertEquals("longxiaochun", payload.get("parameters").get("voice").asText());
      assertEquals("mp3", payload.get("parameters").get("format").asText());

      tts.sendText("第一段");
      JsonNode continueTask = socket.nextText();
      assertEquals("continue-task", continueTask.get("header").get("action").asText());
      assertEquals("第一段", continueTask.get("payload").get("input").get("text").asText());
      // v2 parity: continue-task payload carries input only.
      assertFalse(continueTask.get("payload").has("model"));

      tts.flush();
      JsonNode flushTask = socket.nextText();
      assertTrue(flushTask.get("payload").get("input").get("flush").asBoolean());

      tts.stop();
      JsonNode finishTask = socket.nextText();
      assertEquals("finish-task", finishTask.get("header").get("action").asText());
      assertFalse(finishTask.get("payload").get("input").has("directive"));

      List<byte[]> chunks = new ArrayList<>();
      for (byte[] chunk : tts.audioChunks()) {
        chunks.add(chunk);
      }
      assertEquals(2, chunks.size());
      assertEquals(2, chunks.get(0).length);
      assertEquals(1, chunks.get(1).length);
      assertNotNull(tts.getFirstPackageDelay());
      assertNotNull(tts.getLastPackageDelay());
      assertEquals(3, tts.getUsage().getTotalTokens());
    }
  }

  @Test
  void eventsAndAudioChunksAreMutuallyExclusive() throws Exception {
    TtsServerSocket socket = enqueueUpgrade();
    socket.audioToSend.add(new byte[] {1});
    try (SynthesisSession tts =
        client.audio().tts().connect(TtsConnectParams.builder().model("cosyvoice-v2").build())) {
      tts.start();
      socket.nextText();
      tts.sendText("x");
      tts.stop();

      // An active events() claim blocks both audioChunks() and a second events() reader.
      Iterable<Object> events = tts.events();
      assertThrows(SdkException.class, tts::audioChunks);
      assertThrows(SdkException.class, tts::events);

      // Exhausting the reader releases the claim; both views can then replay the buffer.
      int count = 0;
      for (Object ignored : events) {
        count++;
      }
      assertEquals(2, count); // one audio frame + task-finished

      int audio = 0;
      for (byte[] ignored : tts.audioChunks()) {
        audio++;
      }
      assertEquals(1, audio);
    }
  }

  @Test
  void cancelSendsDirectiveOnceAndNeverAfterTerminal() throws Exception {
    TtsServerSocket socket = enqueueUpgrade();
    try (SynthesisSession tts =
        client.audio().tts().connect(TtsConnectParams.builder().model("cosyvoice-v2").build())) {
      tts.start();
      socket.nextText();
      tts.cancel();
      JsonNode finishTask = socket.nextText();
      assertEquals("finish-task", finishTask.get("header").get("action").asText());
      assertEquals("cancel", finishTask.get("payload").get("input").get("directive").asText());
      // Second cancel and stop: no further frames.
      tts.cancel();
      tts.stop();
      assertNull(socket.texts.poll(300, TimeUnit.MILLISECONDS));
    }
  }

  @Test
  void closeIsNotRestartable() throws Exception {
    enqueueUpgrade();
    SynthesisSession tts =
        client.audio().tts().connect(TtsConnectParams.builder().model("cosyvoice-v2").build());
    tts.start();
    tts.close();
    tts.close();
    SdkException e = assertThrows(SdkException.class, tts::start);
    assertEquals("sdk.ws.IllegalState", e.getCode());
  }

  @Test
  void audioChunksBeforeStartFailsFastInsteadOfSpinning() {
    try (SynthesisSession tts =
        client.audio().tts().connect(TtsConnectParams.builder().model("cosyvoice-v2").build())) {
      // Regression: iterating an unstarted session must fail fast, not busy-spin.
      assertTimeoutPreemptively(
          Duration.ofSeconds(5),
          () -> {
            SdkException e =
                assertThrows(
                    SdkException.class,
                    () -> {
                      for (byte[] ignored : tts.audioChunks()) {
                        // never reached
                      }
                    });
            assertEquals("sdk.ws.IllegalState", e.getCode());
          });
    }
  }
}
