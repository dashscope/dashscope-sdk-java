// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.resources.audio.RealtimeSession;
import com.alibaba.dashscope.types.audio.RealtimeParams;
import com.alibaba.dashscope.types.audio.UpdateSessionParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code audio.realtime} (audio design §1.3/§11). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@ClearEnvironmentVariable(key = "DASHSCOPE_WEBSOCKET_OMNI_BASE_URL")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class RealtimeMockTest {
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
            // Deliberately dead: realtime must NOT honor the ws base-url override (audio design
            // §2).
            .wsBaseUrl("http://127.0.0.1:1/api-ws/v1/inference")
            .timeout(Duration.ofSeconds(5))
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  /** Server-side realtime endpoint: records frames; finishes the session on session.finish. */
  private static final class RealtimeServerSocket extends WebSocketListener {
    final CountDownLatch opened = new CountDownLatch(1);
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final BlockingQueue<JsonNode> texts = new LinkedBlockingQueue<>();

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
        if ("session.finish".equals(frame.path("type").asText())) {
          send("{\"type\":\"session.finished\",\"session\":{\"id\":\"sess-1\"}}");
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

    void send(String json) {
      socket.get().send(json);
    }

    JsonNode nextText() throws InterruptedException {
      JsonNode frame = texts.poll(10, TimeUnit.SECONDS);
      assertNotNull(frame, "timed out waiting for a client text frame");
      return frame;
    }
  }

  private RealtimeServerSocket enqueueUpgrade() {
    RealtimeServerSocket listener = new RealtimeServerSocket();
    server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
    return listener;
  }

  @Test
  void connectDerivesRealtimeUrlFromHttpBaseUrlIgnoringWsOverride() throws Exception {
    RealtimeServerSocket socket = enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build())) {
      socket.awaitOpen();
      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
      assertNotNull(request);
      assertEquals("/api-ws/v1/realtime?model=qwen3-omni-flash-realtime", request.getPath());
    }
  }

  @Test
  void handshakeHonorsRequestOptionsTimeout() {
    // The upgrade never completes: the SDK's own handshake latch must honor the per-request
    // timeout instead of falling back to the default connect timeout.
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
    assertTimeoutPreemptively(
        Duration.ofSeconds(2),
        () -> {
          ApiTimeoutException e =
              assertThrows(
                  ApiTimeoutException.class,
                  () ->
                      client
                          .audio()
                          .realtime()
                          .connect(
                              RealtimeParams.builder().model("qwen3-omni-flash-realtime").build(),
                              RequestOptions.builder().timeout(Duration.ofMillis(200)).build()));
          assertTrue(e.getMessage().contains("websocket handshake after"));
        });
  }

  @Test
  void updateSessionSerializesOnlySetFieldsWithSentinels() throws Exception {
    RealtimeServerSocket socket = enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build())) {
      socket.awaitOpen();

      Map<String, Object> extra = new HashMap<>();
      extra.put("custom_flag", "x");
      extra.put("output_audio_format", null); // explicit-null sentinel
      rt.updateSession(
          UpdateSessionParams.builder()
              .modalities(Arrays.asList("text", "audio"))
              .voice("Cherry")
              .enableTurnDetection(false)
              .enableInputAudioTranscription(true)
              .inputAudioTranscriptionModel("gummy-realtime-v1")
              .parameters(extra)
              .build());

      JsonNode frame = socket.nextText();
      assertEquals("session.update", frame.get("type").asText());
      assertTrue(frame.get("event_id").asText().startsWith("event_"));
      JsonNode session = frame.get("session");
      assertEquals("text", session.get("modalities").get(0).asText());
      assertEquals("Cherry", session.get("voice").asText());
      // enableTurnDetection(false) → explicit JSON null (Python parity)
      assertTrue(session.has("turn_detection") && session.get("turn_detection").isNull());
      assertEquals(
          "gummy-realtime-v1", session.get("input_audio_transcription").get("model").asText());
      assertEquals("x", session.get("custom_flag").asText());
      // explicit-null sentinel from the passthrough map
      assertTrue(session.has("output_audio_format") && session.get("output_audio_format").isNull());
      // unset fields are omitted entirely
      assertFalse(session.has("input_audio_format"));
      assertFalse(session.has("audio"));
      assertFalse(session.has("translation"));
    }
  }

  @Test
  void appendAudioBase64EncodesAndLifecycleFramesFlow() throws Exception {
    RealtimeServerSocket socket = enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build())) {
      socket.awaitOpen();

      rt.appendAudio(new byte[] {1, 2, 3});
      JsonNode append = socket.nextText();
      assertEquals("input_audio_buffer.append", append.get("type").asText());
      assertEquals(
          Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}), append.get("audio").asText());

      rt.commit();
      assertEquals("input_audio_buffer.commit", socket.nextText().get("type").asText());

      rt.createResponse("say hi", Collections.singletonList("text"));
      JsonNode create = socket.nextText();
      assertEquals("response.create", create.get("type").asText());
      assertEquals("say hi", create.get("response").get("instructions").asText());
      assertEquals("text", create.get("response").get("modalities").get(0).asText());

      rt.createResponse();
      JsonNode bare = socket.nextText();
      assertEquals("response.create", bare.get("type").asText());
      assertFalse(bare.has("response"));
    }
  }

  @Test
  void eventsReplayAndTrackSessionMetrics() throws Exception {
    RealtimeServerSocket socket = enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build())) {
      socket.awaitOpen();
      socket.send("{\"type\":\"session.created\",\"session\":{\"id\":\"sess-9\"}}");
      socket.send("{\"type\":\"response.created\",\"response\":{\"id\":\"resp-1\"}}");
      socket.send("{\"type\":\"response.audio.delta\",\"delta\":\"QUJD\"}");
      socket.send("{\"type\":\"response.done\",\"response\":{\"id\":\"resp-1\"}}");

      List<JsonNode> events = new ArrayList<>();
      Iterator<JsonNode> it = rt.events().iterator();
      for (int i = 0; i < 4; i++) {
        assertTrue(it.hasNext());
        events.add(it.next());
      }
      assertEquals("session.created", events.get(0).get("type").asText());
      assertEquals("response.done", events.get(3).get("type").asText());
      assertEquals("sess-9", rt.getSessionId());
      assertEquals("resp-1", rt.getLastResponseId());
      assertNotNull(rt.getFirstAudioDelay());
      assertNull(rt.getFirstTextDelay());

      // response.done seen → a clean remote close ends iteration without error.
      socket.send("{\"type\":\"session.finished\",\"session\":{\"id\":\"sess-9\"}}");
      assertTrue(it.hasNext());
      assertEquals("session.finished", it.next().get("type").asText());
      rt.close();
      assertFalse(it.hasNext());
    }
  }

  @Test
  void earlyDisconnectRaisesConnectionException() throws Exception {
    RealtimeServerSocket socket = enqueueUpgrade();
    RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build());
    socket.awaitOpen();
    socket.send("{\"type\":\"session.created\",\"session\":{\"id\":\"sess-1\"}}");
    socket.socket.get().close(1000, "boom");

    Iterator<JsonNode> it = rt.events().iterator();
    assertTrue(it.hasNext());
    assertEquals("session.created", it.next().get("type").asText());
    ApiConnectionException e = assertThrows(ApiConnectionException.class, it::hasNext);
    assertTrue(e.getMessage().contains("closed before session.finished"));
    rt.close();
  }

  @Test
  void endSessionSendsFinishAndWaitsForFinished() throws Exception {
    RealtimeServerSocket socket = enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build())) {
      socket.awaitOpen();
      // The server auto-replies session.finished upon session.finish (see listener).
      rt.endSession(Duration.ofSeconds(5));
      JsonNode finish = socket.nextText();
      assertEquals("session.finish", finish.get("type").asText());
      assertTrue(finish.get("event_id").asText().startsWith("event_"));
    }
  }

  @Test
  void qwenAudioDialectRejectsEndSession() throws Exception {
    enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen-audio-asr-realtime").build())) {
      assertThrows(UnsupportedOperationException.class, rt::endSession);
      assertThrows(UnsupportedOperationException.class, rt::endSessionAsync);
    }
  }

  @Test
  void staleFailureClosesHandshakeResponseWithoutAffectingNewGeneration() throws Exception {
    RealtimeServerSocket first = enqueueUpgrade();
    try (RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build())) {
      first.awaitOpen();
      Constructor<?> constructor =
          Class.forName(RealtimeSession.class.getName() + "$Listener")
              .getDeclaredConstructor(RealtimeSession.class, int.class);
      constructor.setAccessible(true);
      WebSocketListener oldListener = (WebSocketListener) constructor.newInstance(rt, 1);
      rt.close();

      RealtimeServerSocket second = enqueueUpgrade();
      rt.connect();
      second.awaitOpen();
      AtomicBoolean responseClosed = new AtomicBoolean();
      ResponseBody body =
          new ResponseBody() {
            @Override
            public MediaType contentType() {
              return null;
            }

            @Override
            public long contentLength() {
              return 0;
            }

            @Override
            public BufferedSource source() {
              return new Buffer();
            }

            @Override
            public void close() {
              responseClosed.set(true);
            }
          };
      try (Response response =
          new Response.Builder()
              .request(new Request.Builder().url(server.url("/")).build())
              .protocol(Protocol.HTTP_1_1)
              .code(403)
              .message("Forbidden")
              .body(body)
              .build()) {
        oldListener.onFailure(
            first.socket.get(), new IOException("late handshake failure"), response);
        assertTrue(responseClosed.get());
      }
      oldListener.onFailure(first.socket.get(), new IOException("late transport failure"), null);
      second.send("{\"type\":\"session.created\",\"session\":{\"id\":\"fresh\"}}");
      Iterator<JsonNode> it = rt.events().iterator();
      assertTrue(it.hasNext());
      assertEquals("fresh", it.next().path("session").path("id").asText());
      rt.createResponse();
      assertEquals("response.create", second.nextText().path("type").asText());
    }
  }

  @Test
  void closeThenReconnectDropsStaleGenerationEvents() throws Exception {
    RealtimeServerSocket first = enqueueUpgrade();
    RealtimeSession rt =
        client
            .audio()
            .realtime()
            .connect(RealtimeParams.builder().model("qwen3-omni-flash-realtime").build());
    first.awaitOpen();
    rt.close();

    RealtimeServerSocket second = enqueueUpgrade();
    rt.connect();
    second.awaitOpen();

    // A late event on the old socket must not leak into the new generation.
    first.send("{\"type\":\"session.created\",\"session\":{\"id\":\"stale\"}}");
    second.send("{\"type\":\"session.created\",\"session\":{\"id\":\"fresh\"}}");

    Iterator<JsonNode> it = rt.events().iterator();
    assertTrue(it.hasNext());
    assertEquals("fresh", it.next().get("session").get("id").asText());
    assertEquals("fresh", rt.getSessionId());
    rt.close();
  }
}
