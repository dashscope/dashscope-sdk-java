// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.types.images.ImageGenerateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** MockWebServer tests for {@code images()} (Python v2 images.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class ImagesMockTest {
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
  void legacyGenerateSubmitsAndWaits() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-img-1\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r1\"}"));
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-img-1\",\"task_status\":\"SUCCEEDED\","
                + "\"results\":[{\"url\":\"https://img/1.png\"},{\"url\":\"https://img/2.png\"}]},"
                + "\"usage\":{\"image_count\":2},\"request_id\":\"r2\"}"));

    Job job =
        client
            .images()
            .generate(
                ImageGenerateParams.builder()
                    .model("wanx2.1-t2i-turbo")
                    .prompt("一只猫")
                    .negativePrompt("模糊")
                    .size("1024x1024")
                    .n(2)
                    .seed(42)
                    .style("<auto>")
                    .promptExtend(true)
                    .watermark(false)
                    .parameters(Collections.singletonMap("size", "512*512"))
                    .build());

    assertTrue(job.isSucceeded());
    assertEquals(2, job.getImageUrls().size());
    assertEquals("https://img/1.png", job.getResultUrl());
    // Service-specific counters land in usage extra (design §3).
    assertEquals(2, job.getUsage().get("image_count").asInt());

    RecordedRequest submit = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/text2image/image-synthesis", submit.getPath());
    assertEquals("enable", submit.getHeader("X-DashScope-Async"));
    JsonNode body = bodyOf(submit);
    assertEquals("wanx2.1-t2i-turbo", body.get("model").asText());
    assertEquals("一只猫", body.get("input").get("prompt").asText());
    // negative_prompt lives in input on the legacy protocol.
    assertEquals("模糊", body.get("input").get("negative_prompt").asText());
    JsonNode parameters = body.get("parameters");
    // x normalized to the wire form; the named kwarg wins over parameters.
    assertEquals("1024*1024", parameters.get("size").asText());
    assertEquals(2, parameters.get("n").asInt());
    assertEquals(42, parameters.get("seed").asInt());
    assertEquals("<auto>", parameters.get("style").asText());
    assertTrue(parameters.get("prompt_extend").asBoolean());
    assertFalse(parameters.get("watermark").asBoolean());

    RecordedRequest poll = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/tasks/task-img-1", poll.getPath());
    assertEquals("GET", poll.getMethod());
  }

  @Test
  void multimodalGenerateAnswersSynchronously() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
                + "[{\"image\":\"https://img/a.png\"},{\"image\":\"https://img/b.png\"}]}}]},"
                + "\"usage\":{\"input_tokens\":5},\"request_id\":\"r3\"}"));

    Job job =
        client
            .images()
            .generate(
                ImageGenerateParams.builder()
                    .model("wan2.6-t2i-preview")
                    .prompt("一座山")
                    .imageUrls(Collections.singletonList("https://ref/x.png"))
                    .negativePrompt("雪")
                    .build());

    // The sync answer is wrapped as a finished Job; choices image parts lift to results.
    assertTrue(job.isSucceeded());
    assertEquals(2, job.getImageUrls().size());
    assertEquals("https://img/b.png", job.getImageUrls().get(1));
    assertEquals("r3", job.getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/multimodal-generation/generation", request.getPath());
    // One-shot sync: no async header.
    assertNull(request.getHeader("X-DashScope-Async"));
    JsonNode body = bodyOf(request);
    JsonNode content = body.get("input").get("messages").get(0).get("content");
    assertEquals("user", body.get("input").get("messages").get(0).get("role").asText());
    assertEquals("一座山", content.get(0).get("text").asText());
    assertEquals("https://ref/x.png", content.get(1).get("image").asText());
    // negative_prompt lives in parameters on the multimodal protocol.
    assertEquals("雪", body.get("parameters").get("negative_prompt").asText());
    assertFalse(body.get("input").has("negative_prompt"));
    // No polling: exactly one request.
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void multimodalJobsCreateRoutesToTaskEndpoint() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-img-2\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r4\"}"));

    Job job =
        client
            .images()
            .jobs()
            .create(
                ImageGenerateParams.builder()
                    .model("wan2.6-image")
                    .prompt("p")
                    .imageUrls(Collections.singletonList("oss://already/there.png"))
                    .build());

    assertEquals("task-img-2", job.getTaskId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/image-generation/generation", request.getPath());
    assertEquals("enable", request.getHeader("X-DashScope-Async"));
    // Existing oss:// reference: resolve header without any upload traffic.
    assertEquals("enable", request.getHeader("X-DashScope-OssResourceResolve"));
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void localImageUploadsThenSubmitsWithResolveHeader() throws Exception {
    Path image = Files.createTempFile("img-test", ".png");
    try {
      Files.write(image, new byte[] {1, 2, 3});
      String uploadHost = server.url("/").toString().replaceAll("/$", "");
      server.enqueue(
          okJson(
              "{\"data\":{\"upload_host\":\""
                  + uploadHost
                  + "\",\"oss_access_key_id\":\"ak\","
                  + "\"signature\":\"sig\",\"policy\":\"pol\",\"upload_dir\":\"dir/img\","
                  + "\"x_oss_object_acl\":\"private\",\"x_oss_forbid_overwrite\":\"true\"}}"));
      server.enqueue(new MockResponse().setResponseCode(200).setBody(""));
      server.enqueue(
          okJson(
              "{\"output\":{\"task_id\":\"task-img-3\",\"task_status\":\"PENDING\"},"
                  + "\"request_id\":\"r5\"}"));

      client
          .images()
          .jobs()
          .create(
              ImageGenerateParams.builder()
                  .model("wan2.6-image")
                  .prompt("p")
                  .imageUrls(Collections.singletonList(image.toAbsolutePath().toString()))
                  .build());

      RecordedRequest policy = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("GET", policy.getMethod());
      assertTrue(policy.getPath().startsWith("/uploads?action=getPolicy&model=wan2.6-image"));

      RecordedRequest upload = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("POST", upload.getMethod());
      assertTrue(upload.getBody().readUtf8().contains("name=\"file\""));

      RecordedRequest submit = server.takeRequest(5, TimeUnit.SECONDS);
      assertEquals("enable", submit.getHeader("X-DashScope-OssResourceResolve"));
      JsonNode content = bodyOf(submit).get("input").get("messages").get(0).get("content");
      assertTrue(content.get(1).get("image").asText().startsWith("oss://dir/img/img-test"));
    } finally {
      Files.deleteIfExists(image);
    }
  }

  @Test
  void validationRejectsMissingModelOrPrompt() {
    assertThrows(
        IllegalArgumentException.class, () -> ImageGenerateParams.builder().prompt("p").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> ImageGenerateParams.builder().model("wanx2.1-t2i-turbo").build());
  }

  @Test
  void extraInputMergesIntoInput() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-img-4\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r6\"}"));
    Map<String, Object> extra = new HashMap<>();
    extra.put("ref_strength", 0.6);
    client
        .images()
        .jobs()
        .create(
            ImageGenerateParams.builder()
                .model("wanx2.1-t2i-turbo")
                .prompt("p")
                .function("stylization_all")
                .extraInput(extra)
                .build());
    JsonNode body = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    assertEquals(0.6, body.get("input").get("ref_strength").asDouble());
    assertEquals("stylization_all", body.get("input").get("function").asText());
  }
}
