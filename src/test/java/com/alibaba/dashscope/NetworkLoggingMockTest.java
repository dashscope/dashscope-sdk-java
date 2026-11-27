// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * 3.0 core network logging parity with 2.x: DASHSCOPE_NETWORK_LOGGING_LEVEL drives an OkHttp
 * logging interceptor (shared with the ws handshake path), credential headers are redacted, and the
 * default / invalid levels stay silent. Records are captured from the JUL logger OkHttp's default
 * HttpLoggingInterceptor logger writes to (okhttp3.OkHttpClient, INFO).
 */
@Execution(ExecutionMode.SAME_THREAD)
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class NetworkLoggingMockTest {
  private MockWebServer server;
  private final List<String> lines = new ArrayList<>();
  private Logger julLogger;
  private Handler capture;
  private Level originalLevel;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    capture =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            lines.add(record.getMessage());
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    julLogger = Logger.getLogger("okhttp3.OkHttpClient");
    originalLevel = julLogger.getLevel();
    julLogger.setLevel(Level.INFO);
    julLogger.setUseParentHandlers(false);
    julLogger.addHandler(capture);
  }

  @AfterEach
  void tearDown() throws IOException {
    julLogger.removeHandler(capture);
    julLogger.setLevel(originalLevel);
    julLogger.setUseParentHandlers(true);
    server.shutdown();
  }

  private DashScopeClient newClient() {
    return DashScopeClient.builder()
        .apiKey("test-key")
        .baseUrl(server.url("/").toString())
        .maxRetries(0)
        .build();
  }

  private static ChatCompletionCreateParams params() {
    return ChatCompletionCreateParams.builder()
        .model("qwen-plus")
        .messages(Arrays.asList(Message.user("hi")))
        .build();
  }

  private void createOnce() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":"
                    + "{\"role\":\"assistant\",\"content\":\"hi\"}}]},\"request_id\":\"log1\"}"));
    try (DashScopeClient client = newClient()) {
      client.chat().completions().create(params());
    }
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  void headersLevelLogsRequestsAndRedactsCredentials() {
    createOnce();

    String log = String.join("\n", lines);
    assertTrue(log.contains("--> POST"));
    assertTrue(log.contains("<-- 200"));
    assertTrue(log.contains("Authorization: ██"));
    assertTrue(log.contains("x-dashscope-sdk-client: java-sdk/" + SdkInfo.VERSION + "/aigc3"));
    assertFalse(log.contains("Bearer test-key"));
  }

  @Test
  @ClearEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL")
  void defaultNoneStaysSilent() {
    createOnce();

    String url = server.url("/").toString();
    assertTrue(lines.stream().noneMatch(l -> l.contains(url)));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "VERBOSE")
  void invalidLevelFallsBackToNone() {
    createOnce();

    String url = server.url("/").toString();
    assertTrue(lines.stream().noneMatch(l -> l.contains(url)));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  void bodyLevelLogsUnaryRequestBodies() {
    createOnce();

    String log = String.join("\n", lines);
    assertTrue(log.contains("qwen-plus"));
    assertTrue(log.contains("Authorization: ██"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  void bodyLevelDowngradesToHeadersForSse() {
    // BODY buffers the whole response before releasing it; SSE must stay incremental, so the
    // guard logs it at HEADERS. The payload marker must never reach the log, yet frames parse.
    String marker = "sse-payload-marker";
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                    + "\"content\":\""
                    + marker
                    + "\"}}]},\"request_id\":\"s1\"}\n\n"
                    + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"ok\"},"
                    + "\"finish_reason\":\"stop\"}]},\"request_id\":\"s1\"}\n\n"));

    List<ChatCompletionChunk> chunks = new ArrayList<>();
    try (DashScopeClient client = newClient();
        ResponseStream<ChatCompletionChunk> stream =
            client.chat().completions().createStreaming(params())) {
      for (ChatCompletionChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(2, chunks.size());
    assertEquals(marker, chunks.get(0).delta().getContent());
    String log = String.join("\n", lines);
    assertTrue(log.contains("Accept: text/event-stream"));
    assertFalse(log.contains(marker));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  void bodyLevelSseGuardSurvivesCallerAcceptOverride() {
    // Caller headers are applied after the SDK's streaming Accept, so an overridden Accept must
    // not re-enable BODY buffering: the guard also matches the SDK's X-DashScope-SSE marker.
    String marker = "sse-override-marker";
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                    + "\"content\":\""
                    + marker
                    + "\"}}]},\"request_id\":\"s2\"}\n\n"
                    + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"ok\"},"
                    + "\"finish_reason\":\"stop\"}]},\"request_id\":\"s2\"}\n\n"));

    RequestOptions options = RequestOptions.builder().header("Accept", "application/json").build();
    List<ChatCompletionChunk> chunks = new ArrayList<>();
    try (DashScopeClient client = newClient();
        ResponseStream<ChatCompletionChunk> stream =
            client.chat().completions().createStreaming(params(), options)) {
      for (ChatCompletionChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(2, chunks.size());
    assertEquals(marker, chunks.get(0).delta().getContent());
    assertFalse(String.join("\n", lines).contains(marker));
  }
}
