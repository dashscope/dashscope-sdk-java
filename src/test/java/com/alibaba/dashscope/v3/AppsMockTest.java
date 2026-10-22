// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.errors.RateLimitException;
import com.alibaba.dashscope.v3.types.apps.AppCompletion;
import com.alibaba.dashscope.v3.types.apps.AppCompletionCreateParams;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code apps().completions()} (Python v2 apps.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class AppsMockTest {
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
  void createWithPromptFlattensOutput() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"text\":\"答案\",\"finish_reason\":\"stop\",\"session_id\":\"sess-1\","
                + "\"thoughts\":[{\"thought\":\"想一下\",\"action_name\":\"search\","
                + "\"action_input\":{\"q\":\"x\"},\"custom_field\":1}],"
                + "\"doc_references\":[{\"doc_id\":\"d1\",\"doc_name\":\"文档\","
                + "\"page_number\":[3,4]}],"
                + "\"workflow_message\":{\"node_id\":\"n1\",\"node_type\":\"LLM\","
                + "\"node_is_completed\":\"true\",\"message\":{\"role\":\"assistant\","
                + "\"content\":\"hi\"}}},"
                + "\"usage\":{\"input_tokens\":10,\"output_tokens\":5,\"total_tokens\":15,"
                + "\"models\":[{\"model_id\":\"qwen-plus\",\"input_tokens\":10,"
                + "\"output_tokens\":5}]},\"request_id\":\"req-app-1\"}"));

    AppCompletion result =
        client
            .apps()
            .completions()
            .create(AppCompletionCreateParams.builder().appId("app-1").prompt("问题").build());

    assertFalse(result.isError());
    assertEquals("答案", result.getText());
    assertEquals("stop", result.getFinishReason());
    assertEquals("sess-1", result.getSessionId());
    assertEquals("req-app-1", result.getRequestId());
    assertEquals(200, result.getStatusCode());
    assertEquals(1, result.getThoughts().size());
    assertEquals("想一下", result.getThoughts().get(0).getThought());
    assertEquals("search", result.getThoughts().get(0).getActionName());
    assertEquals("x", result.getThoughts().get(0).getActionInput().get("q"));
    // extra="allow" parity: unmodeled fields are kept, not dropped.
    assertEquals(1, result.getThoughts().get(0).getExtra().get("custom_field"));
    assertEquals("d1", result.getDocReferences().get(0).getDocId());
    assertEquals(Arrays.asList(3, 4), result.getDocReferences().get(0).getPageNumber());
    assertEquals("n1", result.getWorkflowMessage().getNodeId());
    assertEquals("true", result.getWorkflowMessage().getNodeIsCompleted());
    assertEquals("hi", result.getWorkflowMessage().getMessage().getContent());
    assertEquals(15, result.getUsage().getTotalTokens());
    // Per-model detail rides in Usage.extra under "models" (design §3).
    assertNotNull(result.getUsage().getExtra().get("models"));

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/apps/app-1/completion", request.getPath());
    assertEquals("POST", request.getMethod());
    assertEquals("Bearer test-key", request.getHeader("Authorization"));
    JsonNode body = bodyOf(request);
    assertEquals("问题", body.get("input").get("prompt").asText());
    assertFalse(body.get("input").has("messages"));
    assertTrue(body.has("parameters"));
  }

  @Test
  void createWithMessagesAppendsPromptAsUserTurn() throws Exception {
    server.enqueue(okJson("{\"output\":{\"text\":\"ok\"},\"request_id\":\"r2\"}"));

    client
        .apps()
        .completions()
        .create(
            AppCompletionCreateParams.builder()
                .appId("app-2")
                .messages(Collections.singletonList(Message.user("第一轮")))
                .prompt("第二轮")
                .build());

    JsonNode body = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    JsonNode messages = body.get("input").get("messages");
    assertEquals(2, messages.size());
    assertEquals("第一轮", messages.get(0).get("content").asText());
    assertEquals("user", messages.get(1).get("role").asText());
    assertEquals("第二轮", messages.get(1).get("content").asText());
    assertFalse(body.get("input").has("prompt"));
  }

  @Test
  void validationRejectsMissingAppIdAndEmptyInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AppCompletionCreateParams.builder().prompt("q").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> AppCompletionCreateParams.builder().appId("app-1").build());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AppCompletionCreateParams.builder()
                .appId("app-1")
                .messages(Collections.emptyList())
                .build());
  }

  @Test
  void inputOptionsAndParametersMerge() throws Exception {
    server.enqueue(okJson("{\"output\":{\"text\":\"ok\"},\"request_id\":\"r3\"}"));

    client
        .apps()
        .completions()
        .create(
            AppCompletionCreateParams.builder()
                .appId("app-3")
                .prompt("q")
                .sessionId("sess-x")
                .docReferenceType("indexed")
                .docTagCodes(Arrays.asList("tag1", "tag2"))
                .memoryId("mem-1")
                .bizParams(Collections.singletonMap("city", "hz"))
                .imageList(Collections.singletonList("https://img"))
                .fileList(Collections.singletonList("https://file"))
                .temperature(0.5)
                .topP(0.8)
                .topK(10)
                .seed(42)
                .hasThoughts(true)
                .ragOptions(Collections.singletonMap("pipeline_ids", Arrays.asList("p1")))
                .incrementalOutput(true)
                .parameters(Collections.singletonMap("vl_high_resolution_images", true))
                .build());

    JsonNode body = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    JsonNode input = body.get("input");
    assertEquals("sess-x", input.get("session_id").asText());
    assertEquals("indexed", input.get("doc_reference_type").asText());
    assertEquals("tag2", input.get("doc_tag_codes").get(1).asText());
    assertEquals("mem-1", input.get("memory_id").asText());
    assertEquals("hz", input.get("biz_params").get("city").asText());
    assertEquals("https://img", input.get("image_list").get(0).asText());
    assertEquals("https://file", input.get("file_list").get(0).asText());
    JsonNode parameters = body.get("parameters");
    assertEquals(0.5, parameters.get("temperature").asDouble());
    assertEquals(0.8, parameters.get("top_p").asDouble());
    assertEquals(10, parameters.get("top_k").asInt());
    assertEquals(42, parameters.get("seed").asInt());
    assertTrue(parameters.get("has_thoughts").asBoolean());
    assertEquals("p1", parameters.get("rag_options").get("pipeline_ids").get(0).asText());
    assertTrue(parameters.get("incremental_output").asBoolean());
    assertTrue(parameters.get("vl_high_resolution_images").asBoolean());
  }

  @Test
  void streamingYieldsFrames() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"text\":\"你\",\"session_id\":\"s1\"},"
                    + "\"request_id\":\"rs1\"}\n\n"
                    + "data: {\"output\":{\"text\":\"你好\",\"finish_reason\":\"stop\"},"
                    + "\"usage\":{\"input_tokens\":1,\"output_tokens\":2,\"total_tokens\":3},"
                    + "\"request_id\":\"rs1\"}\n\n"));

    List<AppCompletion> frames = new ArrayList<>();
    try (ResponseStream<AppCompletion> stream =
        client
            .apps()
            .completions()
            .createStreaming(
                AppCompletionCreateParams.builder().appId("app-1").prompt("q").build())) {
      for (AppCompletion frame : stream) {
        frames.add(frame);
      }
    }

    assertEquals(2, frames.size());
    assertEquals("你", frames.get(0).getText());
    assertEquals("s1", frames.get(0).getSessionId());
    assertEquals("rs1", frames.get(0).getRequestId());
    assertEquals("你好", frames.get(1).getText());
    assertEquals("stop", frames.get(1).getFinishReason());
    assertEquals(3, frames.get(1).getUsage().getTotalTokens());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("enable", request.getHeader("X-DashScope-SSE"));
    assertEquals("/apps/app-1/completion", request.getPath());
  }

  @Test
  void streamingInStreamErrorFrameThrows() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"text\":\"你\"},\"request_id\":\"rs2\"}\n\n"
                    + "data: {\"code\":\"Throttling\",\"message\":\"throttled\","
                    + "\"request_id\":\"rs2\"}\n\n"));

    try (ResponseStream<AppCompletion> stream =
        client
            .apps()
            .completions()
            .createStreaming(
                AppCompletionCreateParams.builder().appId("app-1").prompt("q").build())) {
      List<String> texts = new ArrayList<>();
      RateLimitException e =
          assertThrows(
              RateLimitException.class,
              () -> {
                for (AppCompletion frame : stream) {
                  texts.add(frame.getText());
                }
              });
      assertEquals("Throttling", e.getCode());
      assertEquals(Collections.singletonList("你"), texts);
    }
  }

  @Test
  void streamingErrorFrameSurfacesAsFinalItemWhenNotRaising() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"code\":\"Throttling\",\"message\":\"throttled\","
                    + "\"request_id\":\"rs3\"}\n\n"));

    try (DashScopeClient lax =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      List<AppCompletion> frames = new ArrayList<>();
      try (ResponseStream<AppCompletion> stream =
          lax.apps()
              .completions()
              .createStreaming(
                  AppCompletionCreateParams.builder().appId("app-1").prompt("q").build())) {
        for (AppCompletion frame : stream) {
          frames.add(frame);
        }
      }
      assertEquals(1, frames.size());
      assertTrue(frames.get(0).isError());
      assertEquals("Throttling", frames.get(0).getCode());
      assertEquals("throttled", frames.get(0).getMessage());
      assertNull(frames.get(0).getText());
    }
  }

  @Test
  void businessErrorOn200RaisesByDefault() {
    server.enqueue(
        okJson("{\"code\":\"InvalidParameter\",\"message\":\"bad app\",\"request_id\":\"r4\"}"));

    assertThrows(
        Exception.class,
        () ->
            client
                .apps()
                .completions()
                .create(AppCompletionCreateParams.builder().appId("app-1").prompt("q").build()));
  }

  @Test
  void businessErrorOn200SurfacesWhenNotRaising() throws Exception {
    server.enqueue(
        okJson("{\"code\":\"InvalidParameter\",\"message\":\"bad app\",\"request_id\":\"r4\"}"));

    try (DashScopeClient lax =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      AppCompletion result =
          lax.apps()
              .completions()
              .create(AppCompletionCreateParams.builder().appId("app-1").prompt("q").build());
      assertTrue(result.isError());
      assertEquals("InvalidParameter", result.getCode());
      assertEquals("bad app", result.getMessage());
      assertEquals("r4", result.getRequestId());
    }
  }
}
