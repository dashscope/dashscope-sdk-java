// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.config.ConfigResolver;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobsService;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.common.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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
class JobsMockTest {
  private MockWebServer server;
  private HttpTransport transport;
  private JobsService jobs;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    transport =
        new HttpTransport(
            ConfigResolver.resolve(
                ClientOptions.builder()
                    .apiKey("test-key")
                    .baseUrl(server.url("/").toString())
                    .maxRetries(0)
                    .build()));
    jobs = new JobsService(transport, "test");
  }

  @AfterEach
  void tearDown() throws IOException {
    transport.close();
    server.shutdown();
  }

  private static MockResponse jobJson(String taskId, String status) {
    return new MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(
            "{\"request_id\":\"req-1\",\"output\":{\"task_id\":\""
                + taskId
                + "\",\"task_status\":\""
                + status
                + "\"}}");
  }

  @Test
  void createSendsAsyncHeaderAndParsesJob() throws Exception {
    server.enqueue(jobJson("task-123", "PENDING"));
    ObjectMapper mapper = new ObjectMapper();
    Job job =
        jobs.create(
            "/services/audio/asr/transcription", mapper.createObjectNode().put("model", "m"), null);
    assertEquals("task-123", job.getTaskId());
    assertEquals("PENDING", job.getTaskStatus());
    assertFalse(job.isFinished());
    assertEquals("req-1", job.getRequestId());

    RecordedRequest request = server.takeRequest();
    assertEquals("enable", request.getHeader("X-DashScope-Async"));
    assertEquals("Bearer test-key", request.getHeader("Authorization"));
  }

  @Test
  void fetchHitsTasksEndpoint() throws Exception {
    server.enqueue(jobJson("task-9", "SUCCEEDED"));
    Job job = jobs.fetch("task-9", null);
    assertTrue(job.isSucceeded());
    assertTrue(job.isFinished());

    RecordedRequest request = server.takeRequest();
    assertEquals("GET", request.getMethod());
    assertEquals("/tasks/task-9", request.getPath());
  }

  @Test
  void waitPollsUntilTerminal() throws Exception {
    server.enqueue(jobJson("task-1", "PENDING"));
    server.enqueue(jobJson("task-1", "RUNNING"));
    server.enqueue(jobJson("task-1", "SUCCEEDED"));
    Job job = jobs.wait("task-1", Duration.ofSeconds(30), null);
    assertTrue(job.isSucceeded());
    assertEquals(3, server.getRequestCount());
  }

  @Test
  void waitTimesOutWithApiTimeout() {
    for (int i = 0; i < 20; i++) {
      server.enqueue(jobJson("task-1", "RUNNING"));
    }
    ApiTimeoutException e =
        assertThrows(
            ApiTimeoutException.class, () -> jobs.wait("task-1", Duration.ofMillis(1500), null));
    assertTrue(e.getMessage().contains("task-1"));
  }

  @Test
  void waitTimeoutBoundsAnInFlightPoll() {
    server.enqueue(
        jobJson("task-slow", "RUNNING").setBodyDelay(5, java.util.concurrent.TimeUnit.SECONDS));
    long started = System.nanoTime();

    assertThrows(
        ApiTimeoutException.class,
        () -> jobs.wait("task-slow", Duration.ofMillis(150), RequestOptions.NONE));

    long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
    assertTrue(elapsedMillis < 2000, "wait exceeded its total budget: " + elapsedMillis + "ms");
  }

  @Test
  void cancelPostsToCancelEndpoint() throws Exception {
    server.enqueue(jobJson("task-2", "CANCELED"));
    Job job = jobs.cancel("task-2", null);
    assertEquals("CANCELED", job.getTaskStatus());

    RecordedRequest request = server.takeRequest();
    assertEquals("POST", request.getMethod());
    assertEquals("/tasks/task-2/cancel", request.getPath());
  }

  @Test
  void listParsesPage() throws Exception {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"request_id\":\"req-2\",\"output\":{\"data\":["
                    + "{\"task_id\":\"t-1\",\"task_status\":\"SUCCEEDED\"},"
                    + "{\"task_id\":\"t-2\",\"task_status\":\"RUNNING\"}],"
                    + "\"total\":2,\"page_no\":1,\"page_size\":20}}"));
    Map<String, String> query = new HashMap<>();
    query.put("page_no", "1");
    query.put("page_size", "20");
    Page<Job> page = jobs.list(query, null);
    assertEquals(2, page.getItems().size());
    assertEquals(2, page.getTotalCount());
    assertEquals(1, page.getPageNo());
    assertEquals("t-1", page.getItems().get(0).getTaskId());
    assertTrue(page.getItems().get(0).isSucceeded());

    RecordedRequest request = server.takeRequest();
    assertEquals("GET", request.getMethod());
    assertNotNull(request.getPath());
    assertTrue(request.getPath().startsWith("/tasks?"));
    assertTrue(request.getPath().contains("page_no=1"));
  }

  @Test
  void jobIdAliasIsHonored() throws Exception {
    server.enqueue(
        new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(
                "{\"request_id\":\"req-3\",\"output\":{\"job_id\":\"job-7\","
                    + "\"task_status\":\"SUCCEEDED\"}}"));
    Job job = jobs.fetch("job-7", null);
    assertEquals("job-7", job.getTaskId());
    assertTrue(job.isSucceeded());
  }
}
