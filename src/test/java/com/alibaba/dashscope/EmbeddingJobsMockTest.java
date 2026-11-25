// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.embeddings.BatchEmbeddingCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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

/** MockWebServer tests for {@code embeddings().jobs()} (Python v2 EmbeddingJobs parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class EmbeddingJobsMockTest {
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
  void createSubmitsAsyncTask() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-emb-1\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r1\"}"));

    Job job =
        client
            .embeddings()
            .jobs()
            .create(
                BatchEmbeddingCreateParams.builder()
                    .model("text-embedding-async-v2")
                    .fileUrl("https://files/batch.txt")
                    .textType("document")
                    .parameters(Collections.singletonMap("dimension", 1024))
                    .build());

    assertEquals("task-emb-1", job.getTaskId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", request.getMethod());
    // Same service path as the sync endpoint, submitted with the async header.
    assertEquals("/services/embeddings/text-embedding/text-embedding", request.getPath());
    assertEquals("enable", request.getHeader("X-DashScope-Async"));
    JsonNode body = bodyOf(request);
    assertEquals("text-embedding-async-v2", body.get("model").asText());
    assertEquals("https://files/batch.txt", body.get("input").get("url").asText());
    assertFalse(body.get("input").has("texts"));
    assertEquals("document", body.get("parameters").get("text_type").asText());
    assertEquals(1024, body.get("parameters").get("dimension").asInt());
  }

  @Test
  void parametersOmittedWhenEmpty() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-emb-2\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r2\"}"));

    client
        .embeddings()
        .jobs()
        .create(
            BatchEmbeddingCreateParams.builder()
                .model("text-embedding-async-v2")
                .fileUrl("https://files/batch.txt")
                .build());

    JsonNode body = bodyOf(server.takeRequest(5, TimeUnit.SECONDS));
    assertFalse(body.has("parameters"));
  }

  @Test
  void validationRejectsMissingModelOrFileUrl() {
    assertThrows(
        IllegalArgumentException.class,
        () -> BatchEmbeddingCreateParams.builder().fileUrl("https://f/x.txt").build());
    assertThrows(
        IllegalArgumentException.class,
        () -> BatchEmbeddingCreateParams.builder().model("text-embedding-async-v2").build());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            BatchEmbeddingCreateParams.builder()
                .model("text-embedding-async-v2")
                .fileUrl("")
                .build());
  }

  @Test
  void waitPollsUntilSucceeded() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-emb-3\",\"task_status\":\"RUNNING\"},"
                + "\"request_id\":\"r3\"}"));
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-emb-3\",\"task_status\":\"SUCCEEDED\","
                + "\"url\":\"https://files/result.txt\"},\"request_id\":\"r4\"}"));

    Job job =
        client.embeddings().jobs().wait("task-emb-3", com.alibaba.dashscope.RequestOptions.NONE);

    assertTrue(job.isSucceeded());
    assertEquals("https://files/result.txt", job.getResultUrl());
    RecordedRequest first = server.takeRequest(5, TimeUnit.SECONDS);
    RecordedRequest second = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/tasks/task-emb-3", first.getPath());
    assertEquals("/tasks/task-emb-3", second.getPath());
  }

  @Test
  void listReturnsServerPage() throws Exception {
    // GET /tasks pages at the top level (no output wrapper); items/total/page_no must survive.
    server.enqueue(
        okJson(
            "{\"request_id\":\"r-list\",\"data\":["
                + "{\"task_id\":\"task-emb-5\",\"task_status\":\"SUCCEEDED\"},"
                + "{\"task_id\":\"task-emb-6\",\"task_status\":\"RUNNING\"}],"
                + "\"total\":6,\"total_page\":3,\"page_no\":2,\"page_size\":2}"));

    Map<String, String> query = new HashMap<>();
    query.put("page_no", "2");
    query.put("page_size", "2");
    Page<Job> page = client.embeddings().jobs().list(query, RequestOptions.NONE);

    assertEquals(2, page.getItems().size());
    assertEquals(6, page.getTotalCount());
    assertEquals(2, page.getPageNo());
    assertEquals(2, page.getPageSize());
    assertEquals("task-emb-5", page.getItems().get(0).getTaskId());
    assertTrue(page.getItems().get(0).isSucceeded());
    assertEquals("r-list", page.getRequestId());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", request.getMethod());
    assertTrue(request.getPath().startsWith("/tasks?"));
    assertTrue(request.getPath().contains("page_no=2"));
  }

  @Test
  void asyncListReturnsServerPage() throws Exception {
    server.enqueue(
        okJson(
            "{\"request_id\":\"r-list-a\",\"data\":["
                + "{\"task_id\":\"task-emb-7\",\"task_status\":\"PENDING\"}],"
                + "\"total\":1,\"page_no\":1,\"page_size\":10}"));
    try (DashScopeAsyncClient async =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      Page<Job> page =
          async
              .embeddings()
              .jobs()
              .list(Collections.emptyMap(), RequestOptions.NONE)
              .get(10, TimeUnit.SECONDS);
      assertEquals(1, page.getItems().size());
      assertEquals(1, page.getTotalCount());
      assertEquals(1, page.getPageNo());
      assertEquals(10, page.getPageSize());
      assertEquals("task-emb-7", page.getItems().get(0).getTaskId());
      assertEquals("r-list-a", page.getRequestId());
    }
  }

  @Test
  void asyncCreateCompletes() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"task_id\":\"task-emb-4\",\"task_status\":\"PENDING\"},"
                + "\"request_id\":\"r5\"}"));
    try (DashScopeAsyncClient async =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      Job job =
          async
              .embeddings()
              .jobs()
              .create(
                  BatchEmbeddingCreateParams.builder()
                      .model("text-embedding-async-v2")
                      .fileUrl("https://files/batch.txt")
                      .build())
              .get(10, TimeUnit.SECONDS);
      assertEquals("task-emb-4", job.getTaskId());
      assertEquals("r5", job.getRequestId());
    }
  }
}
