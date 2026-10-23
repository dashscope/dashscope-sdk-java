// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.ClientOptions;
import com.alibaba.dashscope.v3.config.ConfigResolver;
import com.alibaba.dashscope.v3.resources.audio.AudioService;
import com.alibaba.dashscope.v3.resources.audio.RecognitionSession;
import com.alibaba.dashscope.v3.resources.audio.SynthesisSession;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.ws.WsConnection;
import com.alibaba.dashscope.v3.types.audio.AsrRecognizeParams;
import com.alibaba.dashscope.v3.types.audio.AsrResult;
import com.alibaba.dashscope.v3.types.audio.TtsConnectParams;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * M5 benchmarks (design §9, audio §11): connection-reuse gain and downlink throughput on loopback
 * against MockWebServer. Numbers are printed with a {@code [PERF]} prefix and harvested into
 * md/dashscope-3.0-perf.md; the test asserts only functional correctness, never timing, so it is
 * safe to run in CI.
 */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@ClearEnvironmentVariable(key = "DASHSCOPE_WEBSOCKET_BASE_URL")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class PerfBenchmarkTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final int ASR_CYCLES = 100;
  private static final int HTTP_REQUESTS = 200;
  private static final int HTTP_WARMUP = 20;
  private static final int TTS_CHUNK_BYTES = 48 * 1024;
  private static final int TTS_CHUNKS = 350; // ~16 MB downlink
  private static final Duration RECV = Duration.ofSeconds(30);

  private static final String CHAT_JSON =
      "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\","
          + "\"content\":\"你好\"}}]},\"usage\":{\"input_tokens\":10,\"output_tokens\":5,"
          + "\"total_tokens\":15},\"request_id\":\"req-bench\"}";

  private MockWebServer server;
  private HttpTransport transport;
  private AudioService audio;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    transport =
        new HttpTransport(
            ConfigResolver.resolve(
                ClientOptions.builder()
                    .apiKey("bench-key")
                    .baseUrl(server.url("/").toString())
                    .wsBaseUrl(server.url("/api-ws/v1/inference").toString())
                    .timeout(Duration.ofSeconds(30))
                    .maxRetries(0)
                    .build()));
    audio = new AudioService(transport);
  }

  @AfterEach
  void tearDown() throws IOException {
    transport.close();
    server.shutdown();
  }

  /** Task-protocol endpoint: asr and tts behaviors keyed by payload.function. */
  private static final class BenchSocket extends WebSocketListener {
    private final CountDownLatch opened = new CountDownLatch(1);
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final BlockingQueue<JsonNode> texts = new LinkedBlockingQueue<>();

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
          String function = frame.get("payload").get("function").asText();
          sendEvent(taskId, "task-started", null);
          if ("SpeechSynthesizer".equals(function)) {
            streamAudio(webSocket, taskId);
          } else {
            sendEvent(
                taskId,
                "result-generated",
                "{\"output\":{\"sentence\":{\"begin_time\":0,\"end_time\":100,"
                    + "\"text\":\"你好\",\"sentence_end\":true}}}");
          }
        } else if ("finish-task".equals(action)) {
          sendEvent(taskId, "task-finished", usagePayload());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(code, reason);
    }

    /**
     * Streams the TTS payload on a separate thread with backpressure: okhttp closes the socket when
     * its outgoing queue exceeds 16 MB, so production pauses while more than 4 MB is undelivered.
     */
    private void streamAudio(WebSocket webSocket, String taskId) {
      Thread sender =
          new Thread(
              () -> {
                byte[] chunk = new byte[TTS_CHUNK_BYTES];
                for (int i = 0; i < TTS_CHUNKS; i++) {
                  while (webSocket.queueSize() > 4 * 1024 * 1024) {
                    try {
                      Thread.sleep(1);
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                      return;
                    }
                  }
                  if (!webSocket.send(ByteString.of(chunk, 0, chunk.length))) {
                    return;
                  }
                }
                sendEvent(taskId, "task-finished", usagePayload());
              },
              "bench-tts-sender");
      sender.setDaemon(true);
      sender.start();
    }

    void awaitOpen() throws InterruptedException {
      assertTrue(opened.await(10, TimeUnit.SECONDS), "bench server socket never opened");
    }

    JsonNode nextText() throws InterruptedException {
      JsonNode frame = texts.poll(10, TimeUnit.SECONDS);
      assertNotNull(frame, "timed out waiting for a client text frame");
      return frame;
    }

    private static String usagePayload() {
      return "{\"output\":{},\"usage\":{\"input_tokens\":1,\"output_tokens\":2,"
          + "\"total_tokens\":3}}";
    }

    private void sendEvent(String taskId, String event, String payloadJson) {
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
  }

  private BenchSocket enqueueUpgrade() {
    BenchSocket listener = new BenchSocket();
    server.enqueue(new MockResponse().withWebSocketUpgrade(listener));
    return listener;
  }

  private static AsrRecognizeParams asrParams() {
    return AsrRecognizeParams.builder()
        .model("paraformer-realtime-v2")
        .format("pcm")
        .sampleRate(16000)
        .build();
  }

  private long oneRecognizeCycle(WsConnection shared) throws IOException {
    long start = System.nanoTime();
    RecognitionSession asr =
        shared != null
            ? audio.asr().recognize(asrParams(), shared)
            : audio.asr().recognize(asrParams());
    try {
      asr.start();
      asr.sendAudioFrame(new byte[] {1, 2, 3, 4});
      asr.stop();
      int results = 0;
      for (AsrResult r : asr.results()) {
        results++;
        assertEquals("你好", r.getSentence().getText());
      }
      assertEquals(1, results);
      assertNotNull(asr.getUsage());
    } finally {
      asr.close();
    }
    return System.nanoTime() - start;
  }

  @Test
  void asrRecognizeSharedVsDedicatedConnection() throws Exception {
    BenchSocket sharedSocket = enqueueUpgrade();
    WsConnection shared =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null);
    sharedSocket.awaitOpen();
    for (int i = 0; i < 5; i++) {
      oneRecognizeCycle(shared); // warmup
    }
    List<Long> sharedNanos = new ArrayList<>();
    for (int i = 0; i < ASR_CYCLES; i++) {
      sharedNanos.add(oneRecognizeCycle(shared));
    }
    shared.close();

    List<Long> dedicatedNanos = new ArrayList<>();
    for (int i = 0; i < ASR_CYCLES; i++) {
      BenchSocket socket = enqueueUpgrade();
      dedicatedNanos.add(oneRecognizeCycle(null));
      socket.awaitOpen();
    }

    report("asr recognize, shared connection", sharedNanos);
    report("asr recognize, dedicated connection per task", dedicatedNanos);
  }

  @Test
  void httpKeepAliveVsNoReuse() throws Exception {
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder()
            .model("qwen-plus")
            .messages(Collections.singletonList(Message.user("你好")))
            .build();

    try (com.alibaba.dashscope.DashScopeClient warmClient =
        com.alibaba.dashscope.DashScopeClient.builder()
            .apiKey("bench-key")
            .baseUrl(server.url("/").toString())
            .timeout(Duration.ofSeconds(30))
            .maxRetries(0)
            .build()) {
      for (int i = 0; i < HTTP_WARMUP + HTTP_REQUESTS; i++) {
        server.enqueue(okChat());
      }
      for (int i = 0; i < HTTP_WARMUP; i++) {
        chatOnce(warmClient, params);
      }
      List<Long> keepAliveNanos = new ArrayList<>();
      for (int i = 0; i < HTTP_REQUESTS; i++) {
        keepAliveNanos.add(chatOnce(warmClient, params));
      }
      report("http chat, keep-alive pool", keepAliveNanos);
    }

    // A pool that evicts idle connections immediately forces a fresh TCP handshake per request.
    OkHttpClient noReuseHttp =
        new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(0, 1, TimeUnit.NANOSECONDS))
            .build();
    try (com.alibaba.dashscope.DashScopeClient coldClient =
        com.alibaba.dashscope.DashScopeClient.builder()
            .apiKey("bench-key")
            .baseUrl(server.url("/").toString())
            .timeout(Duration.ofSeconds(30))
            .maxRetries(0)
            .httpClient(noReuseHttp)
            .build()) {
      for (int i = 0; i < HTTP_REQUESTS; i++) {
        server.enqueue(okChat());
      }
      List<Long> noReuseNanos = new ArrayList<>();
      for (int i = 0; i < HTTP_REQUESTS; i++) {
        noReuseNanos.add(chatOnce(coldClient, params));
      }
      report("http chat, no connection reuse", noReuseNanos);
    }
  }

  private static long chatOnce(
      com.alibaba.dashscope.DashScopeClient client, ChatCompletionCreateParams params) {
    long start = System.nanoTime();
    assertEquals("你好", client.chat().completions().create(params).outputText());
    return System.nanoTime() - start;
  }

  private static MockResponse okChat() {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(CHAT_JSON);
  }

  @Test
  void ttsDownlinkThroughputOverSharedConnection() throws Exception {
    BenchSocket socket = enqueueUpgrade();
    try (WsConnection shared =
        new WsConnection(transport, transport.config().getWsBaseUrl(), "audio", null)) {
      socket.awaitOpen();
      // warmup
      runTtsAndDrain(shared);
      long start = System.nanoTime();
      long bytes = runTtsAndDrain(shared);
      double seconds = (System.nanoTime() - start) / 1e9;
      double mb = bytes / 1024.0 / 1024.0;
      System.out.println(
          String.format(
              Locale.ROOT,
              "[PERF] tts downlink over shared connection: %.1f MB in %.3f s = %.1f MB/s",
              mb,
              seconds,
              mb / seconds));
    }
  }

  private long runTtsAndDrain(WsConnection shared) throws IOException {
    try (SynthesisSession tts =
        audio
            .tts()
            .connect(
                TtsConnectParams.builder()
                    .model("cosyvoice-v2")
                    .voice("longxiaochun")
                    .format("mp3")
                    .build(),
                shared)) {
      tts.start();
      tts.sendText("吞吐基准");
      tts.flush();
      long bytes = 0;
      int chunks = 0;
      for (byte[] chunk : tts.audioChunks()) {
        bytes += chunk.length;
        chunks++;
      }
      assertEquals(TTS_CHUNKS, chunks);
      assertEquals((long) TTS_CHUNKS * TTS_CHUNK_BYTES, bytes);
      assertNotNull(tts.getUsage());
      return bytes;
    }
  }

  private static void report(String label, List<Long> nanos) {
    long[] sorted = nanos.stream().mapToLong(Long::longValue).sorted().toArray();
    double avgMs = Arrays.stream(sorted).average().orElse(0) / 1e6;
    double p50Ms = sorted[(int) (sorted.length * 0.50)] / 1e6;
    double p99Ms = sorted[Math.min(sorted.length - 1, (int) (sorted.length * 0.99))] / 1e6;
    double minMs = sorted[0] / 1e6;
    System.out.println(
        String.format(
            Locale.ROOT,
            "[PERF] %s: n=%d avg=%.3f ms p50=%.3f ms p99=%.3f ms min=%.3f ms",
            label,
            sorted.length,
            avgMs,
            p50Ms,
            p99Ms,
            minMs));
  }
}
