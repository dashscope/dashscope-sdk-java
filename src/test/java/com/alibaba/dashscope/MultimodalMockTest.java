// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.BadRequestException;
import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.multimodal.ContentPart;
import com.alibaba.dashscope.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCompletion;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCreateParams;
import com.alibaba.dashscope.types.multimodal.MultimodalEmbeddingCreateParams;
import com.alibaba.dashscope.types.multimodal.MultimodalEmbeddingResponse;
import com.alibaba.dashscope.types.multimodal.MultimodalMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
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

@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class MultimodalMockTest {
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

  private static MultimodalChatCreateParams chatParams() {
    return MultimodalChatCreateParams.builder()
        .model("qwen-vl-plus")
        .messages(
            Arrays.asList(
                MultimodalMessage.user(
                    Arrays.asList(
                        ContentPart.text("描述这张图"), ContentPart.image("http://img/x.png")))))
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
  void createKeepsContentPartsAndFlattensEnvelope() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\","
                + "\"content\":[{\"text\":\"一只猫\"},{\"text\":\"在睡觉\"}]}}]},"
                + "\"usage\":{\"input_tokens\":10,\"output_tokens\":5,\"total_tokens\":15,"
                + "\"image_tokens\":8},\"request_id\":\"mm-1\"}"));

    MultimodalChatCompletion result = client.multimodal().chat().create(chatParams());

    assertEquals("mm-1", result.getRequestId());
    assertEquals("一只猫在睡觉", result.outputText());
    assertEquals(2, result.firstChoice().getMessage().getContent().size());
    assertEquals("一只猫", result.firstChoice().getMessage().getContent().get(0).getText());
    assertEquals(8, result.getUsage().getExtra().get("image_tokens"));
    assertEquals(15, result.getUsage().getTotalTokens());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/multimodal-generation/generation", request.getPath());
    assertEquals(
        "java-sdk/" + SdkInfo.VERSION + "/aigc", request.getHeader("x-dashscope-sdk-client"));
    JsonNode body = bodyOf(request);
    assertEquals("qwen-vl-plus", body.get("model").asText());
    JsonNode content = body.get("input").get("messages").get(0).get("content");
    assertEquals("描述这张图", content.get(0).get("text").asText());
    assertEquals("http://img/x.png", content.get(1).get("image").asText());
    assertEquals("message", body.get("parameters").get("result_format").asText());
    assertFalse(body.get("parameters").has("incremental_output"));
  }

  /**
   * M2 acceptance: incremental merge semantics match 2.x. incremental_output=false on a
   * merge-capable model flips the wire value to true, tags the UA suffix, and each emitted chunk
   * carries cumulative text / reasoning / tool_calls.
   */
  @Test
  void streamingMergesIncrementalFramesLike2x() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                    + "\"content\":[{\"text\":\"你\"}],\"reasoning_content\":\"思考\","
                    + "\"tool_calls\":[{\"index\":0,\"id\":\"call_1\",\"type\":\"function\","
                    + "\"function\":{\"name\":\"get_\",\"arguments\":\"{\\\"city\\\"\"}}]}}]},"
                    + "\"request_id\":\"m1\"}\n\n"
                    + "data: {\"output\":{\"choices\":[{\"message\":{"
                    + "\"content\":[{\"text\":\"好\"}],\"reasoning_content\":\"中\","
                    + "\"tool_calls\":[{\"index\":0,\"type\":\"function\","
                    + "\"function\":{\"name\":\"weather\",\"arguments\":\":\\\"hz\\\"\"}}]},"
                    + "\"finish_reason\":\"tool_calls\"}]},\"request_id\":\"m1\"}\n\n"));

    MultimodalChatCreateParams params =
        MultimodalChatCreateParams.builder()
            .model("qwen-vl-plus")
            .messages(Arrays.asList(MultimodalMessage.user(Arrays.asList(ContentPart.text("hi")))))
            .incrementalOutput(false)
            .build();

    List<MultimodalChatChunk> chunks = new ArrayList<>();
    try (ResponseStream<MultimodalChatChunk> stream =
        client.multimodal().chat().createStreaming(params)) {
      for (MultimodalChatChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(2, chunks.size());
    // Frame 1: cumulative equals the delta itself.
    assertEquals("你", chunks.get(0).firstChoice().getMessage().getContent().get(0).getText());
    assertEquals("思考", chunks.get(0).firstChoice().getMessage().getReasoningContent());
    // Frame 2: text and reasoning are cumulative; tool call function fields are concatenated.
    assertEquals("你好", chunks.get(1).firstChoice().getMessage().getContent().get(0).getText());
    assertEquals("思考中", chunks.get(1).firstChoice().getMessage().getReasoningContent());
    assertEquals("tool_calls", chunks.get(1).firstChoice().getFinishReason());
    JsonNode toolCalls =
        MAPPER.valueToTree(chunks.get(1).firstChoice().getMessage().getToolCalls());
    assertEquals(1, toolCalls.size());
    assertEquals("call_1", toolCalls.get(0).get("id").asText());
    assertEquals("get_weather", toolCalls.get(0).get("function").get("name").asText());
    assertEquals("{\"city\":\"hz\"", toolCalls.get(0).get("function").get("arguments").asText());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    JsonNode body = bodyOf(request);
    assertTrue(body.get("parameters").get("incremental_output").asBoolean());
    assertTrue(request.getHeader("user-agent").contains("incremental_to_full/1"));
  }

  @Test
  void streamingRealIncrementalPassesThroughUnmerged() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data:"
                    + " {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":[{\"text\":\"你\"}]}}]},\"request_id\":\"m2\"}\n\n"
                    + "data:"
                    + " {\"output\":{\"choices\":[{\"message\":{\"content\":[{\"text\":\"好\"}]}}]},\"request_id\":\"m2\"}\n\n"));

    MultimodalChatCreateParams params =
        MultimodalChatCreateParams.builder()
            .model("qwen-vl-plus")
            .messages(Arrays.asList(MultimodalMessage.user(Arrays.asList(ContentPart.text("hi")))))
            .incrementalOutput(true)
            .build();

    List<MultimodalChatChunk> chunks = new ArrayList<>();
    try (ResponseStream<MultimodalChatChunk> stream =
        client.multimodal().chat().createStreaming(params)) {
      for (MultimodalChatChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(2, chunks.size());
    assertEquals("你", chunks.get(0).firstChoice().getMessage().getContent().get(0).getText());
    assertEquals("好", chunks.get(1).firstChoice().getMessage().getContent().get(0).getText());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertTrue(bodyOf(request).get("parameters").get("incremental_output").asBoolean());
    assertTrue(request.getHeader("user-agent").contains("incremental_to_full/0"));
  }

  @Test
  void omniModelNeverMergesEvenWhenIncrementalFalse() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "data:"
                    + " {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":[{\"text\":\"你\"}]}}]},\"request_id\":\"m3\"}\n\n"
                    + "data:"
                    + " {\"output\":{\"choices\":[{\"message\":{\"content\":[{\"text\":\"你好\"}]}}]},\"request_id\":\"m3\"}\n\n"));

    MultimodalChatCreateParams params =
        MultimodalChatCreateParams.builder()
            .model("qwen-omni-turbo")
            .messages(Arrays.asList(MultimodalMessage.user(Arrays.asList(ContentPart.text("hi")))))
            .incrementalOutput(false)
            .build();

    List<MultimodalChatChunk> chunks = new ArrayList<>();
    try (ResponseStream<MultimodalChatChunk> stream =
        client.multimodal().chat().createStreaming(params)) {
      for (MultimodalChatChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    // No merge: second frame keeps the server-provided cumulative text, wire flag stays false.
    assertEquals("你", chunks.get(0).firstChoice().getMessage().getContent().get(0).getText());
    assertEquals("你好", chunks.get(1).firstChoice().getMessage().getContent().get(0).getText());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertFalse(bodyOf(request).get("parameters").get("incremental_output").asBoolean());
    assertTrue(request.getHeader("user-agent").contains("incremental_to_full/0"));
  }

  @Test
  void streamingSkipsUsageOnlyFramesAndFallsBackToHeaderRequestId() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setHeader("x-request-id", "request-from-header")
            .setBody(
                "data: {\"usage\":{\"input_tokens\":3,\"output_tokens\":0}}\n\n"
                    + "data: {\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                    + "\"content\":[{\"text\":\"正常结果\"}]}}]}}\n\n"
                    + "data: {\"usage\":{\"input_tokens\":3,\"output_tokens\":4}}\n\n"));

    List<MultimodalChatChunk> chunks = new ArrayList<>();
    try (ResponseStream<MultimodalChatChunk> stream =
        client.multimodal().chat().createStreaming(chatParams())) {
      for (MultimodalChatChunk chunk : stream) {
        chunks.add(chunk);
      }
    }

    assertEquals(1, chunks.size());
    assertEquals("正常结果", chunks.get(0).firstChoice().getMessage().getContent().get(0).getText());
    assertEquals("request-from-header", chunks.get(0).getRequestId());
  }

  @Test
  void streamingStillEmitsOutputlessErrorFrameWhenNotRaising() throws Exception {
    DashScopeClient nonRaising =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build();
    try {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setHeader("x-request-id", "request-from-header")
              .setBody(
                  "data: {\"usage\":{\"input_tokens\":3,\"output_tokens\":0}}\n\n"
                      + "data: {\"code\":\"InvalidParameter\",\"message\":\"bad input\"}\n\n"));

      List<MultimodalChatChunk> chunks = new ArrayList<>();
      try (ResponseStream<MultimodalChatChunk> stream =
          nonRaising.multimodal().chat().createStreaming(chatParams())) {
        for (MultimodalChatChunk chunk : stream) {
          chunks.add(chunk);
        }
      }

      assertEquals(1, chunks.size());
      assertEquals("InvalidParameter", chunks.get(0).getCode());
      assertEquals("request-from-header", chunks.get(0).getRequestId());
      assertTrue(chunks.get(0).isError());
    } finally {
      nonRaising.close();
    }
  }

  @Test
  void localImageIsUploadedAndReplacedWithOssUrl() throws Exception {
    Path image = Files.createTempFile("mm-test", ".png");
    Files.write(image, new byte[] {1, 2, 3});
    try {
      String uploadHost = server.url("/").toString().replaceAll("/$", "");
      server.enqueue(
          okJson(
              "{\"data\":{\"upload_host\":\""
                  + uploadHost
                  + "\",\"oss_access_key_id\":\"ak\","
                  + "\"signature\":\"sig\",\"policy\":\"pol\",\"upload_dir\":\"dir/mm\","
                  + "\"x_oss_object_acl\":\"private\",\"x_oss_forbid_overwrite\":\"true\"}}"));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(""));
      server.enqueue(
          okJson(
              "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                  + "\"content\":[{\"text\":\"ok\"}]}}]},\"request_id\":\"mm-oss\"}"));

      MultimodalChatCreateParams params =
          MultimodalChatCreateParams.builder()
              .model("qwen-vl-plus")
              .messages(
                  Arrays.asList(
                      MultimodalMessage.user(
                          Arrays.asList(ContentPart.image(image.toAbsolutePath().toString())))))
              .build();

      MultimodalChatCompletion result = client.multimodal().chat().create(params);
      assertEquals("ok", result.outputText());

      RecordedRequest policy = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("GET", policy.getMethod());
      assertTrue(policy.getPath().startsWith("/uploads?action=getPolicy&model="));

      RecordedRequest upload = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("POST", upload.getMethod());
      String multipart = upload.getBody().readUtf8();
      assertTrue(multipart.contains("name=\"OSSAccessKeyId\""));
      assertTrue(multipart.contains("name=\"policy\""));
      assertTrue(multipart.contains("name=\"file\""));

      RecordedRequest generation = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("enable", generation.getHeader("X-DashScope-OssResourceResolve"));
      JsonNode content =
          bodyOf(generation).get("input").get("messages").get(0).get("content").get(0);
      assertTrue(content.get("image").asText().startsWith("oss://dir/mm/mm-test"));
    } finally {
      Files.deleteIfExists(image);
    }
  }

  @Test
  void missingLocalFileRaisesInvalidFile() {
    MultimodalChatCreateParams params =
        MultimodalChatCreateParams.builder()
            .model("qwen-vl-plus")
            .messages(
                Arrays.asList(
                    MultimodalMessage.user(
                        Arrays.asList(ContentPart.image("file:///nonexistent/x.png")))))
            .build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> client.multimodal().chat().create(params));
    // INVALID_FILE's catalog code is BadRequestError (400 family).
    assertEquals("BadRequestError", e.getCode());
    assertEquals(0, server.getRequestCount());
  }

  @Test
  void ossUploadFailureIsNotRetried() throws Exception {
    // The upload is non-idempotent (x-oss-forbid-overwrite): even with retries configured, the
    // multipart POST must go out exactly once — a lost response after a successful store would
    // otherwise surface as a bogus 403 FileAlreadyExists.
    Path image = Files.createTempFile("mm-noretry", ".png");
    Files.write(image, new byte[] {1, 2, 3});
    DashScopeClient retrying =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .timeout(Duration.ofSeconds(2))
            .maxRetries(2)
            .build();
    try {
      String uploadHost = server.url("/").toString().replaceAll("/$", "");
      server.enqueue(
          okJson(
              "{\"data\":{\"upload_host\":\""
                  + uploadHost
                  + "\",\"oss_access_key_id\":\"ak\","
                  + "\"signature\":\"sig\",\"policy\":\"pol\",\"upload_dir\":\"dir/mm\","
                  + "\"x_oss_object_acl\":\"private\",\"x_oss_forbid_overwrite\":\"true\"}}"));
      server.enqueue(new MockResponse().setResponseCode(500).setBody("oss broken"));

      MultimodalChatCreateParams params =
          MultimodalChatCreateParams.builder()
              .model("qwen-vl-plus")
              .messages(
                  Arrays.asList(
                      MultimodalMessage.user(
                          Arrays.asList(ContentPart.image(image.toAbsolutePath().toString())))))
              .build();

      ApiStatusException e =
          assertThrows(ApiStatusException.class, () -> retrying.multimodal().chat().create(params));
      assertEquals(500, e.getStatusCode());
      // One policy GET + exactly one upload POST.
      assertEquals(2, server.getRequestCount());
    } finally {
      retrying.close();
      Files.deleteIfExists(image);
    }
  }

  @Test
  void policyResponseMissingFieldRaisesDescriptiveError() throws Exception {
    Path image = Files.createTempFile("mm-bad-policy", ".png");
    Files.write(image, new byte[] {1});
    try {
      // No "signature" in the policy payload: fail with the field name, not an NPE.
      server.enqueue(
          okJson(
              "{\"data\":{\"upload_host\":\"http://localhost\",\"oss_access_key_id\":\"ak\","
                  + "\"policy\":\"pol\",\"upload_dir\":\"dir/mm\","
                  + "\"x_oss_object_acl\":\"private\",\"x_oss_forbid_overwrite\":\"true\"}}"));

      MultimodalChatCreateParams params =
          MultimodalChatCreateParams.builder()
              .model("qwen-vl-plus")
              .messages(
                  Arrays.asList(
                      MultimodalMessage.user(
                          Arrays.asList(ContentPart.image(image.toAbsolutePath().toString())))))
              .build();

      ApiStatusException e =
          assertThrows(ApiStatusException.class, () -> client.multimodal().chat().create(params));
      assertTrue(e.getMessage().contains("signature"));
      // Only the policy GET was issued; no upload attempt.
      assertEquals(1, server.getRequestCount());
    } finally {
      Files.deleteIfExists(image);
    }
  }

  @Test
  void ossUrlPassesThroughAndEnablesResolveHeader() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"content\":[{\"text\":\"ok\"}]}}]},\"request_id\":\"mm-passthrough\"}"));

    MultimodalChatCreateParams params =
        MultimodalChatCreateParams.builder()
            .model("qwen-vl-plus")
            .messages(
                Arrays.asList(
                    MultimodalMessage.user(
                        Arrays.asList(ContentPart.image("oss://already/there.png")))))
            .build();

    client.multimodal().chat().create(params);

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("enable", request.getHeader("X-DashScope-OssResourceResolve"));
    JsonNode content = bodyOf(request).get("input").get("messages").get(0).get("content").get(0);
    assertEquals("oss://already/there.png", content.get("image").asText());
    // No policy fetch, no upload.
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void embeddingsCreateFlattensAndKeepsUsageExtra() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"embeddings\":[{\"index\":0,\"type\":\"text\","
                + "\"embedding\":[0.1,0.2,0.3]}]},"
                + "\"usage\":{\"input_tokens\":2,\"image_count\":0},\"request_id\":\"me-1\"}"));

    MultimodalEmbeddingResponse response =
        client
            .multimodal()
            .embeddings()
            .create(
                MultimodalEmbeddingCreateParams.builder()
                    .model("multimodal-embedding-v1")
                    .contents(Arrays.asList(ContentPart.text("你好")))
                    .build());

    assertEquals("me-1", response.getRequestId());
    assertEquals(1, response.getEmbeddings().size());
    assertEquals("text", response.getEmbeddings().get(0).getType());
    assertEquals(3, response.getEmbeddings().get(0).getEmbedding().size());
    assertEquals(2, response.getUsage().getInputTokens());
    assertNotNull(response.getUsage().getExtra().get("image_count"));

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals(
        "/services/embeddings/multimodal-embedding/multimodal-embedding", request.getPath());
    assertEquals(
        "java-sdk/" + SdkInfo.VERSION + "/embeddings", request.getHeader("x-dashscope-sdk-client"));
    JsonNode body = bodyOf(request);
    assertEquals("multimodal-embedding-v1", body.get("model").asText());
    assertEquals("你好", body.get("input").get("contents").get(0).get("text").asText());
  }
}
