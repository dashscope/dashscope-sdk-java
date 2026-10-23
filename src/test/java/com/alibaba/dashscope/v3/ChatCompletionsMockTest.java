// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.v3.errors.ApiStatusException;
import com.alibaba.dashscope.v3.errors.BadRequestException;
import com.alibaba.dashscope.v3.errors.ContentFilterException;
import com.alibaba.dashscope.v3.errors.RateLimitException;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import com.alibaba.dashscope.v3.types.completions.ChatCompletion;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
    assertTrue(request.getHeader("user-agent").startsWith("dashscope/" + SdkInfo.VERSION));
    assertEquals(
        "java-sdk/" + SdkInfo.VERSION + "/aigc", request.getHeader("x-dashscope-sdk-client"));
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
        assertThrows(
            BadRequestException.class, () -> client.chat().completions().create(params()));
    assertEquals("InvalidParameter", e.getCode());
    assertEquals(400, e.getStatusCode());
    assertEquals("r4", e.getRequestId());
    assertEquals("bad param", e.getMessage());
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void businessCodeLayerMapsToContentFilter() {
    server.enqueue(
        okJson(
            "{\"code\":\"DataInspectionFailed\",\"message\":\"unsafe\",\"request_id\":\"r5\"}"));

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
      assertTrue(request.getHeader("user-agent").startsWith("dashscope/" + SdkInfo.VERSION));
      assertNotNull(request.getHeader("x-dashscope-sdk-session-id"));
      assertEquals(
          "java-sdk/" + SdkInfo.VERSION + "/aigc", request.getHeader("x-dashscope-sdk-client"));
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
                .build());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("Bearer custom-key", request.getHeader("Authorization"));
    assertEquals("custom-agent/1.0", request.getHeader("User-Agent"));
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
              new Flow.Subscriber<ChatCompletionChunk>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                  subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ChatCompletionChunk item) {
                  items.add(item);
                }

                @Override
                public void onError(Throwable throwable) {
                  error.set(throwable);
                  done.countDown();
                }

                @Override
                public void onComplete() {
                  done.countDown();
                }
              });
      assertTrue(done.await(10, TimeUnit.SECONDS));
      assertNull(error.get());
      assertEquals(2, items.size());
      assertEquals("好", items.get(1).delta().getContent());
    }
  }
}
