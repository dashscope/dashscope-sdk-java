// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.BadRequestException;
import com.alibaba.dashscope.errors.ContentFilterException;
import com.alibaba.dashscope.errors.RateLimitException;
import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.GzipSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class ChatCompletionsMockTest {
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
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  private static ChatCompletionCreateParams params() {
    return ChatCompletionCreateParams.builder()
        .model("qwen-plus")
        .messages(Arrays.asList(Message.user("你好")))
        .build();
  }

  private static JsonNode bodyOf(RecordedRequest request) throws IOException {
    return MAPPER.readTree(request.getBody().readUtf8());
  }

  private static MockResponse okJson(String json) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(json);
  }

  private static Buffer gzipped(String text) throws IOException {
    Buffer body = new Buffer();
    try (GzipSink gzip = new GzipSink(body)) {
      Buffer plain = new Buffer().writeUtf8(text);
      gzip.write(plain, plain.size());
    }
    return body;
  }

  @Test
  void createHappyPath() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\","
                + "\"content\":\"你好\"}}]},\"usage\":{\"input_tokens\":10,\"output_tokens\":5,"
                + "\"total_tokens\":15},\"request_id\":\"req-abc\"}"));

    ChatCompletion result = client.chat().completions().create(params());

    assertEquals("你好", result.outputText());
    assertEquals("req-abc", result.getRequestId());
    assertEquals(200, result.getStatusCode());
    assertFalse(result.isError());
    assertEquals(10, result.getUsage().getInputTokens());
    assertEquals(5, result.getUsage().getOutputTokens());
    assertEquals(15, result.getUsage().getTotalTokens());
    assertEquals("stop", result.getChoices().get(0).getFinishReason());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", request.getMethod());
    assertEquals("/services/aigc/text-generation/generation", request.getPath());
    assertEquals("Bearer test-key", request.getHeader("Authorization"));
    assertTrue(request.getHeader("user-agent").startsWith("dashscope-sdk-java/" + SdkInfo.VERSION));
    assertEquals(
        "java-sdk/" + SdkInfo.VERSION + "/aigc3", request.getHeader("x-dashscope-sdk-client"));
    assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));

    JsonNode body = bodyOf(request);
    assertEquals("qwen-plus", body.get("model").asText());
    JsonNode messages = body.get("input").get("messages");
    assertEquals(1, messages.size());
    assertEquals("user", messages.get(0).get("role").asText());
    assertEquals("你好", messages.get(0).get("content").asText());
    assertEquals("message", body.get("parameters").get("result_format").asText());
    assertFalse(body.get("parameters").has("incremental_output"));
  }

  @Test
  void toolResultSerializesToolCallId() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"content\":\"done\"}}]},\"request_id\":\"tool-1\"}"));
    ChatCompletionCreateParams toolParams =
        ChatCompletionCreateParams.builder()
            .model("qwen-plus")
            .messages(Arrays.asList(Message.tool("sunny", "get_weather", "call-42")))
            .build();

    client.chat().completions().create(toolParams);

    JsonNode message = bodyOf(server.takeRequest()).get("input").get("messages").get(0);
    assertEquals("tool", message.get("role").asText());
    assertEquals("get_weather", message.get("name").asText());
    assertEquals("call-42", message.get("tool_call_id").asText());
  }

  @Test
  void asyncClientUsesInjectedExecutor() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"content\":\"async\"}}]},\"request_id\":\"async-1\"}"));
    AtomicInteger executions = new AtomicInteger();
    try (DashScopeAsyncClient async =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .executor(
                command -> {
                  executions.incrementAndGet();
                  command.run();
                })
            .build()) {
      ChatCompletion result = async.chat().completions().create(params()).get(5, TimeUnit.SECONDS);
      assertEquals("async", result.outputText());
      assertEquals(1, executions.get());
    }
  }

  @Test
  void gzipResponseKeepsContentEncodingHeader() throws Exception {
    // OkHttp transparently decompresses gzip and strips Content-Encoding/Content-Length from
    // the application-layer response; the public headers must still mirror the wire response.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setHeader("Content-Encoding", "gzip")
            .setBody(
                gzipped(
                    "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                        + "\"content\":\"你好\"}}]},\"request_id\":\"gz1\"}")));

    ChatCompletion result = client.chat().completions().create(params());

    assertEquals("你好", result.outputText());
    assertEquals("gzip", result.getHeaders().get("Content-Encoding"));
    assertNotNull(result.getHeaders().get("Content-Length"));

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("gzip", request.getHeader("Accept-Encoding"));
  }

  @Test
  void asyncGzipResponseKeepsContentEncodingHeader() throws Exception {
    try (DashScopeAsyncClient async =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "application/json")
              .setHeader("Content-Encoding", "gzip")
              .setBody(
                  gzipped(
                      "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                          + "\"content\":\"async\"}}]},\"request_id\":\"gz2\"}")));

      ChatCompletion result = async.chat().completions().create(params()).get(5, TimeUnit.SECONDS);

      assertEquals("async", result.outputText());
      assertEquals("gzip", result.getHeaders().get("Content-Encoding"));
    }
  }

  @Test
  void gzipStreamingDecodesAndErrorHeadersKeepContentEncoding() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setHeader("Content-Encoding", "gzip")
            .setBody(
                gzipped(
                    "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]},"
                        + "\"request_id\":\"gz3\"}\n\n"
                        + "data: {\"code\":\"Throttling\",\"message\":\"throttled\","
                        + "\"request_id\":\"gz3\"}\n\n")));

    try (ResponseStream<ChatCompletionChunk> stream =
        client.chat().completions().createStreaming(params())) {
      List<String> contents = new ArrayList<>();
      RateLimitException e =
          assertThrows(
              RateLimitException.class,
              () -> {
                for (ChatCompletionChunk chunk : stream) {
                  contents.add(chunk.delta().getContent());
                }
              });
      assertEquals(Arrays.asList("你"), contents);
      assertEquals("gzip", e.getHeaders().get("Content-Encoding"));
    }
  }

  @Test
  void createLegacyTextFormatSynthesizesChoice() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"text\":\"你好世界\",\"finish_reason\":\"stop\"},\"request_id\":\"r2\"}"));

    ChatCompletion result = client.chat().completions().create(params());

    assertEquals("你好世界", result.outputText());
    assertEquals("assistant", result.getChoices().get(0).getMessage().getRole());
    assertEquals("stop", result.getChoices().get(0).getFinishReason());
  }

  @Test
  void workspaceHeaderSentWhenConfigured() throws Exception {
    try (DashScopeClient wsClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .workspace("ws-1")
            .maxRetries(0)
            .build()) {
      server.enqueue(
          okJson(
              "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"x\"}}]},"
                  + "\"request_id\":\"r3\"}"));
      wsClient.chat().completions().create(params());

      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("ws-1", request.getHeader("X-DashScope-WorkSpace"));
    }
  }

  @Test
  void badRequestMapsToBadRequestExceptionWithoutRetry() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"code\":\"InvalidParameter\",\"message\":\"bad param\",\"request_id\":\"r4\"}"));

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> client.chat().completions().create(params()));
    assertEquals("InvalidParameter", e.getCode());
    assertEquals(400, e.getStatusCode());
    assertEquals("r4", e.getRequestId());
    assertEquals("bad param", e.getMessage());
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void businessCodeLayerMapsToContentFilter() {
    server.enqueue(
        okJson("{\"code\":\"DataInspectionFailed\",\"message\":\"unsafe\",\"request_id\":\"r5\"}"));

    ContentFilterException e =
        assertThrows(
            ContentFilterException.class, () -> client.chat().completions().create(params()));
    assertEquals("DataInspectionFailed", e.getCode());
    assertEquals(200, e.getStatusCode());
    assertTrue(e instanceof BadRequestException);
  }

  @Test
  void unknownBusinessCodeLandsOnPlainApiStatusException() {
    server.enqueue(okJson("{\"code\":\"Foo.Bar\",\"message\":\"mystery\"}"));

    ApiStatusException e =
        assertThrows(ApiStatusException.class, () -> client.chat().completions().create(params()));
    assertEquals(ApiStatusException.class, e.getClass());
    assertEquals("Foo.Bar", e.getCode());
    assertEquals(200, e.getStatusCode());
  }

  @Test
  void raiseOnErrorFalseReturnsErrorEnvelope() {
    try (DashScopeClient laxClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      server.enqueue(
          okJson("{\"code\":\"InvalidParameter\",\"message\":\"bad\",\"request_id\":\"r6\"}"));
      ChatCompletion businessError = laxClient.chat().completions().create(params());
      assertTrue(businessError.isError());
      assertEquals("InvalidParameter", businessError.getCode());
      assertEquals(200, businessError.getStatusCode());
      assertEquals("r6", businessError.getRequestId());

      server.enqueue(
          new MockResponse()
              .setResponseCode(400)
              .setHeader("Content-Type", "application/json")
              .setBody("{\"code\":\"InvalidParameter\",\"message\":\"bad\"}"));
      ChatCompletion httpError = laxClient.chat().completions().create(params());
      assertTrue(httpError.isError());
      assertEquals(400, httpError.getStatusCode());
    }
  }

  @Test
  void retryAfter429ThenSuccess() throws Exception {
    try (DashScopeClient retryClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(2)
            .build()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(429)
              .setHeader("Retry-After-ms", "1")
              .setHeader("Content-Type", "application/json")
              .setBody("{\"code\":\"Throttling\",\"message\":\"slow down\"}"));
      server.enqueue(
          okJson(
              "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]},"
                  + "\"request_id\":\"r7\"}"));

      ChatCompletion result = retryClient.chat().completions().create(params());
      assertEquals("ok", result.outputText());
      assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  void rateLimitExhaustionThrows() {
    try (DashScopeClient retryClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(1)
            .build()) {
      for (int i = 0; i < 2; i++) {
        server.enqueue(
            new MockResponse()
                .setResponseCode(429)
                .setHeader("Retry-After-ms", "1")
                .setHeader("Content-Type", "application/json")
                .setBody("{\"code\":\"Throttling.RateQuota\",\"message\":\"throttled\"}"));
      }

      RateLimitException e =
          assertThrows(
              RateLimitException.class, () -> retryClient.chat().completions().create(params()));
      assertEquals(429, e.getStatusCode());
      assertEquals("Throttling.RateQuota", e.getCode());
      assertEquals(2, server.getRequestCount());
    }
  }

  @Test
  void injectedHttpClientStillGetsAuthAndTelemetryHeaders() throws Exception {
    try (DashScopeClient injectedClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .httpClient(new OkHttpClient())
            .maxRetries(0)
            .build()) {
      server.enqueue(
          okJson(
              "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hi\"}}]},"
                  + "\"request_id\":\"inj1\"}"));

      ChatCompletion result = injectedClient.chat().completions().create(params());
      assertEquals("hi", result.outputText());

      RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("Bearer test-key", request.getHeader("Authorization"));
      assertTrue(
          request.getHeader("user-agent").startsWith("dashscope-sdk-java/" + SdkInfo.VERSION));
      assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));
      assertEquals(
          "java-sdk/" + SdkInfo.VERSION + "/aigc3", request.getHeader("x-dashscope-sdk-client"));
    }
  }

  @Test
  void raiseOnErrorFalseWithUnparseableErrorBodyStillReportsError() {
    try (DashScopeClient laxClient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      // Gateway HTML error page: no JSON, no code — the envelope must still isError().
      server.enqueue(
          new MockResponse()
              .setResponseCode(502)
              .setHeader("Content-Type", "text/html")
              .setBody("<html><body>Bad Gateway</body></html>"));

      ChatCompletion result = laxClient.chat().completions().create(params());
      assertTrue(result.isError());
      assertEquals(502, result.getStatusCode());
      assertEquals("HTTP_502", result.getCode());
      assertNotNull(result.getMessage());
    }
  }

  @Test
  void customRequestOptionsHeadersOverrideSdkSetHeaders() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hi\"}}]},"
                + "\"request_id\":\"ovr1\"}"));

    client
        .chat()
        .completions()
        .create(
            params(),
            RequestOptions.builder()
                .header("Authorization", "Bearer custom-key")
                .header("User-Agent", "custom-agent/1.0")
                .header("x-dashscope-sdk-client", "custom")
                .build());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("Bearer custom-key", request.getHeader("Authorization"));
    assertEquals("custom-agent/1.0", request.getHeader("User-Agent"));
    assertEquals("custom", request.getHeader("x-dashscope-sdk-client"));
  }

  @Test
  void streamingYieldsIncrementalChunks() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                    + "\"content\":\"你\"}}]},\"request_id\":\"s1\"}\n\n"
                    + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"好\"},"
                    + "\"finish_reason\":\"stop\"}]},\"usage\":{\"input_tokens\":1,"
                    + "\"output_tokens\":2,\"total_tokens\":3},\"request_id\":\"s1\"}\n\n"));

    List<ChatCompletionChunk> chunks = new ArrayList<>();
    try (ResponseStream<ChatCompletionChunk> stream =
        client.chat().completions().createStreaming(params())) {
      for (ChatCompletionChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(2, chunks.size());
    assertEquals("你", chunks.get(0).delta().getContent());
    assertEquals("assistant", chunks.get(0).delta().getRole());
    assertEquals("好", chunks.get(1).delta().getContent());
    assertEquals("stop", chunks.get(1).getChoices().get(0).getFinishReason());
    assertEquals(3, chunks.get(1).getUsage().getTotalTokens());
    assertEquals("s1", chunks.get(0).getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("enable", request.getHeader("X-DashScope-SSE"));
    assertEquals("text/event-stream", request.getHeader("Accept"));
    JsonNode body = bodyOf(request);
    assertTrue(body.get("parameters").get("incremental_output").asBoolean());
    assertEquals("message", body.get("parameters").get("result_format").asText());
  }

  @Test
  void streamingChunksCarryStreamResponseHeaders() throws Exception {
    // Frames deliberately omit request_id: the envelope must fall back to the stream headers.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setHeader("X-Request-Id", "hdr-s1")
            .setBody(
                "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]}}\n\n"
                    + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"好\"},"
                    + "\"finish_reason\":\"stop\"}]}}\n\n"));

    List<ChatCompletionChunk> chunks = new ArrayList<>();
    try (ResponseStream<ChatCompletionChunk> stream =
        client.chat().completions().createStreaming(params())) {
      for (ChatCompletionChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(2, chunks.size());
    for (ChatCompletionChunk chunk : chunks) {
      assertEquals("text/event-stream", chunk.getHeaders().get("Content-Type"));
      assertEquals("hdr-s1", chunk.getHeaders().get("X-Request-Id"));
      assertEquals("hdr-s1", chunk.getRequestId());
      assertEquals(200, chunk.getStatusCode());
    }
  }

  @Test
  void asyncStreamingChunksCarryStreamResponseHeaders() throws Exception {
    try (DashScopeAsyncClient async =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setHeader("X-Request-Id", "hdr-a1")
              .setBody(
                  "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]}}\n\n"
                      + "data:"
                      + " {\"output\":{\"choices\":[{\"message\":{\"content\":\"好\"}}]}}\n\n"));

      List<ChatCompletionChunk> items = new ArrayList<>();
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<Throwable> error = new AtomicReference<>();
      async
          .chat()
          .completions()
          .createStreaming(params())
          .subscribe(
              items::add,
              t -> {
                error.set(t);
                done.countDown();
              },
              done::countDown);
      assertTrue(done.await(10, TimeUnit.SECONDS));
      assertNull(error.get());
      assertEquals(2, items.size());
      for (ChatCompletionChunk chunk : items) {
        assertEquals("text/event-stream", chunk.getHeaders().get("Content-Type"));
        assertEquals("hdr-a1", chunk.getHeaders().get("X-Request-Id"));
        assertEquals("hdr-a1", chunk.getRequestId());
      }
    }
  }

  @Test
  void streamingInStreamErrorFrameThrows() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]},"
                    + "\"request_id\":\"s2\"}\n\n"
                    + "data: {\"code\":\"Throttling\",\"message\":\"throttled\","
                    + "\"request_id\":\"s2\"}\n\n"));

    try (ResponseStream<ChatCompletionChunk> stream =
        client.chat().completions().createStreaming(params())) {
      List<String> contents = new ArrayList<>();
      RateLimitException e =
          assertThrows(
              RateLimitException.class,
              () -> {
                for (ChatCompletionChunk chunk : stream) {
                  contents.add(chunk.delta().getContent());
                }
              });
      assertEquals("Throttling", e.getCode());
      assertEquals(Arrays.asList("你"), contents);
    }
  }

  @Test
  void streamingErrorFrameIsTerminalWhenNotRaising() throws Exception {
    try (DashScopeClient lax =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      // The server keeps sending content after the mid-stream error frame; the error item
      // must be the last one delivered.
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody(
                  "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]},"
                      + "\"request_id\":\"s3\"}\n\n"
                      + "data: {\"code\":\"Throttling\",\"message\":\"throttled\","
                      + "\"request_id\":\"s3\"}\n\n"
                      + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"好\"}}]},"
                      + "\"request_id\":\"s3\"}\n\n"));
      List<ChatCompletionChunk> chunks = new ArrayList<>();
      try (ResponseStream<ChatCompletionChunk> stream =
          lax.chat().completions().createStreaming(params())) {
        for (ChatCompletionChunk chunk : stream) {
          chunks.add(chunk);
        }
      }
      assertEquals(2, chunks.size());
      assertEquals("你", chunks.get(0).delta().getContent());
      assertFalse(chunks.get(0).isError());
      assertTrue(chunks.get(1).isError());
      assertEquals("Throttling", chunks.get(1).getCode());
      assertEquals("throttled", chunks.get(1).getMessage());
    }
  }

  @Test
  void asyncStreamingErrorFrameIsTerminalWhenNotRaising() throws Exception {
    try (DashScopeAsyncClient laxAsync =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody(
                  "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]},"
                      + "\"request_id\":\"a3\"}\n\n"
                      + "data: {\"code\":\"Throttling\",\"message\":\"throttled\","
                      + "\"request_id\":\"a3\"}\n\n"
                      + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"好\"}}]},"
                      + "\"request_id\":\"a3\"}\n\n"));
      List<ChatCompletionChunk> items = new ArrayList<>();
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<Throwable> error = new AtomicReference<>();
      laxAsync
          .chat()
          .completions()
          .createStreaming(params())
          .subscribe(
              items::add,
              t -> {
                error.set(t);
                done.countDown();
              },
              done::countDown);
      assertTrue(done.await(10, TimeUnit.SECONDS));
      // Compat mode completes normally after the error item; no trailing content, no onError.
      assertNull(error.get());
      assertEquals(2, items.size());
      assertEquals("你", items.get(0).delta().getContent());
      assertTrue(items.get(1).isError());
      assertEquals("Throttling", items.get(1).getCode());
    }
  }

  @Test
  void streamingInitialHttpFailureAlwaysThrows() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"code\":\"InvalidParameter\",\"message\":\"bad\"}"));

    assertThrows(
        BadRequestException.class, () -> client.chat().completions().createStreaming(params()));
  }

  @Test
  void asyncClientCreateAndStreaming() throws Exception {
    try (DashScopeAsyncClient asyncClient =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      server.enqueue(
          okJson(
              "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"hi\"}}]},"
                  + "\"request_id\":\"a1\"}"));
      ChatCompletion created =
          asyncClient.chat().completions().create(params()).get(10, TimeUnit.SECONDS);
      assertEquals("hi", created.outputText());

      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody(
                  "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"你\"}}]},"
                      + "\"request_id\":\"a2\"}\n\n"
                      + "data: {\"output\":{\"choices\":[{\"message\":{\"content\":\"好\"}}]},"
                      + "\"request_id\":\"a2\"}\n\n"));

      List<ChatCompletionChunk> items = new ArrayList<>();
      CountDownLatch done = new CountDownLatch(1);
      AtomicReference<Throwable> error = new AtomicReference<>();
      asyncClient
          .chat()
          .completions()
          .createStreaming(params())
          .subscribe(
              items::add,
              t -> {
                error.set(t);
                done.countDown();
              },
              done::countDown);
      assertTrue(done.await(10, TimeUnit.SECONDS));
      assertNull(error.get());
      assertEquals(2, items.size());
      assertEquals("好", items.get(1).delta().getContent());
    }
  }
}
