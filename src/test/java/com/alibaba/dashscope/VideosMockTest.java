// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.types.common.Page;
import com.alibaba.dashscope.v3.types.videos.VideoGenerateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

/** MockWebServer tests for {@code videos()} (Python v2 videos.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class VideosMockTest {
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
            "{\"output\":{\"task_id\":\"task-vid-1\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r1\"}"));
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-vid-1\",\"task_status\":\"SUCCEEDED\","
                + "\"video_url\":\"https://vid/1.mp4\"},\"request_id\":\"r2\"}"));

    Job job =
        client
            .videos()
            .generate(
                VideoGenerateParams.builder()
                    .model("wanx2.1-t2v-turbo")
                    .prompt("一只猫")
                    .imageUrl("https://ref/first.png")
                    .size("1280x720")
                    .duration(5)
                    .seed(7)
                    .promptExtend(true)
                    .watermark(true)
                    .parameters(Collections.singletonMap("duration", 3))
                    .build());

    assertTrue(job.isSucceeded());
    assertEquals("https://vid/1.mp4", job.getVideoUrl());
    assertEquals("https://vid/1.mp4", job.getResultUrl());

    RecordedRequest submit = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/video-generation/video-synthesis", submit.getPath());
    assertEquals("enable", submit.getHeader("X-DashScope-Async"));
    JsonNode body = bodyOf(submit);
    assertEquals("wanx2.1-t2v-turbo", body.get("model").asText());
    assertEquals("一只猫", body.get("input").get("prompt").asText());
    // Legacy wire spelling; the public name is imageUrl.
    assertEquals("https://ref/first.png", body.get("input").get("img_url").asText());
    assertFalse(body.get("input").has("media"));
    JsonNode parameters = body.get("parameters");
    // Videos do NOT normalize x -> * (unlike images).
    assertEquals("1280x720", parameters.get("size").asText());
    // Named kwargs win over the parameters map.
    assertEquals(5, parameters.get("duration").asInt());
    assertEquals(7, parameters.get("seed").asInt());
    assertTrue(parameters.get("prompt_extend").asBoolean());
    assertTrue(parameters.get("watermark").asBoolean());

    RecordedRequest poll = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/tasks/task-vid-1", poll.getPath());
    assertEquals("GET", poll.getMethod());
  }

  @Test
  void mediaProtocolBuildsMediaList() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-vid-2\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r3\"}"));

    List<Map<String, Object>> media =
        Arrays.asList(
            mapOf("type", "last_frame", "url", "oss://dir/last.png"),
            mapOf("type", "driving_audio", "url", "oss://dir/voice.mp3"));
    Job job =
        client
            .videos()
            .jobs()
            .create(
                VideoGenerateParams.builder()
                    .model("wan2.7-i2v")
                    .imageUrl("oss://dir/first.png")
                    .media(media)
                    .resolution("1080P")
                    .ratio("16:9")
                    .build());

    assertEquals("task-vid-2", job.getTaskId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/aigc/video-generation/video-synthesis", request.getPath());
    // Existing oss:// references: resolve header without any upload traffic.
    assertEquals("enable", request.getHeader("X-DashScope-OssResourceResolve"));
    JsonNode body = bodyOf(request);
    JsonNode items = body.get("input").get("media");
    // image_url is shorthand and lands first as a first_frame asset.
    assertEquals("first_frame", items.get(0).get("type").asText());
    assertEquals("oss://dir/first.png", items.get(0).get("url").asText());
    assertEquals("last_frame", items.get(1).get("type").asText());
    assertEquals("driving_audio", items.get(2).get("type").asText());
    assertFalse(body.get("input").has("img_url"));
    assertEquals("1080P", body.get("parameters").get("resolution").asText());
    assertEquals("16:9", body.get("parameters").get("ratio").asText());
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void mediaProtocolValidation() {
    // imageUrl duplicating an explicit first_frame media item.
    assertThrows(
        IllegalArgumentException.class,
        () ->
            VideoGenerateParams.builder()
                .model("wan2.7-i2v")
                .imageUrl("https://ref/first.png")
                .media(
                    Collections.singletonList(
                        mapOf("type", "first_frame", "url", "https://ref/f.png")))
                .build());
    // Media-protocol model with neither media nor imageUrl.
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () -> VideoGenerateParams.builder().model("wan2.7-i2v").prompt("p").build());
    assertTrue(e.getMessage().contains("wan2.7-i2v"));
  }

  @Test
  void legacyRequiresSomeInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> VideoGenerateParams.builder().model("wanx2.1-t2v-turbo").build());
  }

  @Test
  void extraInputAndFunctionMergeIntoInput() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-vid-3\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r4\"}"));
    Map<String, Object> extra = new HashMap<>();
    extra.put("ref_strength", 0.5);
    client
        .videos()
        .jobs()
        .create(
            VideoGenerateParams.builder()
                .model("wanx2.1-t2v-turbo")
                .prompt("p")
                .function("video_editing")
                .extraInput(extra)
                .build());
    JsonNode body = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    assertEquals(0.5, body.get("input").get("ref_strength").asDouble());
    assertEquals("video_editing", body.get("input").get("function").asText());
    // An empty merge produces no parameters node at all.
    assertNull(body.get("parameters"));
  }

  @Test
  void cancelAndList() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-vid-4\",\"task_status\":\"CANCELED\"},"
                + "\"request_id\":\"r5\"}"));
    server.enqueue(
        okJson(
            "{\"output\":{\"data\":[{\"task_id\":\"t1\",\"task_status\":\"SUCCEEDED\"},"
                + "{\"task_id\":\"t2\",\"task_status\":\"RUNNING\"}],"
                + "\"total\":2,\"page_no\":1,\"page_size\":10},\"request_id\":\"r6\"}"));

    Job canceled = client.videos().jobs().cancel("task-vid-4", RequestOptions.NONE);
    assertEquals(Job.CANCELED, canceled.getTaskStatus());
    assertTrue(canceled.isFinished());
    RecordedRequest cancel = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", cancel.getMethod());
    assertEquals("/tasks/task-vid-4/cancel", cancel.getPath());

    Map<String, String> query = new HashMap<>();
    query.put("page_no", "1");
    query.put("page_size", "10");
    query.put("model_name", "wanx2.1-t2v-turbo");
    Page<Job> page = client.videos().jobs().list(query, RequestOptions.NONE);
    assertEquals(2, page.getTotalCount());
    assertEquals(2, page.getItems().size());
    assertEquals("t1", page.getItems().get(0).getTaskId());
    assertEquals("r6", page.getRequestId());
    RecordedRequest list = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", list.getMethod());
    assertTrue(list.getPath().startsWith("/tasks?"));
    assertTrue(list.getPath().contains("page_no=1"));
    assertTrue(list.getPath().contains("model_name=wanx2.1-t2v-turbo"));
  }

  private static Map<String, Object> mapOf(String k1, Object v1, String k2, Object v2) {
    Map<String, Object> map = new HashMap<>();
    map.put(k1, v1);
    map.put(k2, v2);
    return map;
  }
}
