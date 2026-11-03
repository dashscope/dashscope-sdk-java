// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.errors.BadRequestException;
import com.alibaba.dashscope.resources.finetunes.FineTuneEventStream;
import com.alibaba.dashscope.types.common.JobStatus;
import com.alibaba.dashscope.types.finetunes.FineTune;
import com.alibaba.dashscope.types.finetunes.FineTuneCreateParams;
import com.alibaba.dashscope.types.finetunes.FineTuneEvent;
import com.alibaba.dashscope.types.finetunes.FineTuneList;
import com.alibaba.dashscope.types.finetunes.JobLogs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
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

/** MockWebServer tests for {@code fineTunes()} (Python v2 fine_tunes.py parity). */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class FineTunesMockTest {
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
  void createPostsBody() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"job_id\":\"ft-1\",\"status\":\"PENDING\","
                + "\"base_model\":\"qwen-turbo\"},\"request_id\":\"r1\"}"));

    FineTune job =
        client
            .fineTunes()
            .create(
                FineTuneCreateParams.builder()
                    .model("qwen-turbo")
                    .trainingFileIds(Collections.singletonList("file-1"))
                    .mode("lora")
                    .hyperParameters(Collections.singletonMap("n_epochs", 2))
                    .finetunedModel("my-model")
                    .build());

    assertEquals("ft-1", job.getJobId());
    assertEquals("ft-1", job.getTaskId());
    assertEquals("PENDING", job.getStatus());
    assertEquals("r1", job.getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", request.getMethod());
    assertEquals("/fine-tunes", request.getPath());
    JsonNode body = bodyOf(request);
    assertEquals("qwen-turbo", body.get("model").asText());
    assertEquals("file-1", body.get("training_file_ids").get(0).asText());
    // Absent validation files ride as an explicit null (Python parity).
    assertTrue(body.has("validation_file_ids") && body.get("validation_file_ids").isNull());
    assertEquals(2, body.get("hyper_parameters").get("n_epochs").asInt());
    assertEquals("lora", body.get("training_type").asText());
    // The produced model is named via model_name, never finetuned_output.
    assertEquals("my-model", body.get("model_name").asText());
    assertFalse(body.has("finetuned_output"));
  }

  @Test
  void getParsesFlattenedJob() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"job_id\":\"ft-2\",\"status\":\"RUNNING\",\"usage\":1234,"
                + "\"finetuned_output\":\"m\",\"create_time\":\"2026-10-01\"},"
                + "\"request_id\":\"r2\"}"));
    FineTune job = client.fineTunes().get("ft-2");
    assertEquals("RUNNING", job.getStatus());
    // Bare-number usage normalizes to the canonical shape.
    assertEquals(1234, job.getUsage().getTotalTokens());
    // Wire aliases land on the public names.
    assertEquals("m", job.getFinetunedModel());
    assertEquals("2026-10-01", job.getCreatedAt());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", request.getMethod());
    assertEquals("/fine-tunes/ft-2", request.getPath());
  }

  @Test
  void listParsesJobsWithPerJobErrors() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"jobs\":[{\"job_id\":\"j1\",\"status\":\"SUCCEEDED\"},"
                + "{\"job_id\":\"j2\",\"status\":\"FAILED\",\"code\":\"InternalError\","
                + "\"message\":\"boom\"}],\"page_no\":1,\"page_size\":10,\"total\":2},"
                + "\"request_id\":\"r3\"}"));
    FineTuneList page = client.fineTunes().list(1, 10);
    assertEquals(2, page.getTotalCount());
    assertEquals(2, page.getJobs().size());
    FineTune failed = page.getJobs().get(1);
    assertEquals("j2", failed.getJobId());
    // The per-job code fills the nested envelope.
    assertTrue(failed.isError());
    assertEquals("boom", failed.getMessage());
    assertEquals("r3", page.getRequestId());
    assertEquals(
        "/fine-tunes?page_no=1&page_size=10", server.takeRequest(5, TimeUnit.SECONDS).getPath());
  }

  @Test
  void cancelAndDeleteReturnJobStatus() throws Exception {
    server.enqueue(okJson("{\"output\":{\"status\":\"CANCELED\"},\"request_id\":\"r4\"}"));
    server.enqueue(okJson("{\"output\":{\"status\":\"DELETED\"},\"request_id\":\"r5\"}"));

    JobStatus canceled = client.fineTunes().cancel("ft-3");
    assertEquals("CANCELED", canceled.getStatus());
    assertEquals("r4", canceled.getRequestId());
    RecordedRequest cancel = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", cancel.getMethod());
    assertEquals("/fine-tunes/ft-3/cancel", cancel.getPath());

    JobStatus deleted = client.fineTunes().delete("ft-3");
    assertEquals("DELETED", deleted.getStatus());
    RecordedRequest delete = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("DELETE", delete.getMethod());
    assertEquals("/fine-tunes/ft-3", delete.getPath());
  }

  @Test
  void logsNormalizesArrayContent() throws Exception {
    server.enqueue(okJson("{\"output\":{\"logs\":[\"l1\",null,\"l3\"]},\"request_id\":\"r6\"}"));
    JobLogs logs = client.fineTunes().logs("ft-4", 5, 100);
    assertEquals("ft-4", logs.getJobId());
    assertEquals("l1\n\nl3", logs.getContent());
    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    // The wire parameter is singular "line".
    assertEquals("/fine-tunes/ft-4/logs?offset=5&line=100", request.getPath());
  }

  @Test
  void streamEventsParsesFrames() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody("data: {\"loss\": 0.1}\n\ndata: {\"loss\": 0.05}\n\n"));

    List<FineTuneEvent> events = new ArrayList<>();
    try (FineTuneEventStream stream = client.fineTunes().streamEvents("ft-5")) {
      for (FineTuneEvent event : stream) {
        events.add(event);
      }
    }
    assertEquals(2, events.size());
    assertFalse(events.get(0).isError());
    assertEquals(0.1, events.get(0).getOutput().get("loss").asDouble());
    assertEquals(0.05, events.get(1).getOutput().get("loss").asDouble());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", request.getMethod());
    assertEquals("/fine-tunes/ft-5/stream", request.getPath());
    assertEquals("text/event-stream", request.getHeader("Accept"));
  }

  @Test
  void streamEventsErrorFrameRaisesOrTerminates() throws Exception {
    String errorStream =
        "data: {\"loss\": 0.1}\n\nevent:error\nstatus: 400\n"
            + "data: {\"code\":\"InvalidParameter\",\"message\":\"bad job\"}\n\n";

    // raiseOnError: the error frame throws the catalog-mapped exception.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(errorStream));
    try (FineTuneEventStream stream = client.fineTunes().streamEvents("ft-6")) {
      BadRequestException e =
          assertThrows(
              BadRequestException.class,
              () -> {
                for (FineTuneEvent ignored : stream) {
                  // drain
                }
              });
      assertEquals("bad job", e.getMessage());
      assertEquals(400, e.getStatusCode().intValue());
    }

    // raiseOnError(false): the error frame surfaces as the final error-carrying event.
    try (DashScopeClient lenient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      server.enqueue(
          new MockResponse()
              .setResponseCode(200)
              .setHeader("Content-Type", "text/event-stream")
              .setBody(errorStream));
      List<FineTuneEvent> events = new ArrayList<>();
      try (FineTuneEventStream stream = lenient.fineTunes().streamEvents("ft-6")) {
        for (FineTuneEvent event : stream) {
          events.add(event);
        }
      }
      assertEquals(2, events.size());
      assertFalse(events.get(0).isError());
      assertTrue(events.get(1).isError());
      assertEquals("bad job", events.get(1).getOutput().get("message").asText());
    }
  }

  @Test
  void streamEventsErrorWithoutOwnStatusStaysBusinessFailure() throws Exception {
    // A status: line on an earlier frame must not leak into a later error frame that omits its
    // own: statusCode==null means business failure on an HTTP-200 stream (JobEventSupport), so
    // the mapped exception reports 200, not the inherited 404.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody(
                "status: 404\ndata: {\"loss\": 0.1}\n\nevent:error\n"
                    + "data: {\"code\":\"InvalidParameter\",\"message\":\"bad job\"}\n\n"));
    try (FineTuneEventStream stream = client.fineTunes().streamEvents("ft-9")) {
      ApiStatusException e =
          assertThrows(
              ApiStatusException.class,
              () -> {
                for (FineTuneEvent ignored : stream) {
                  // drain
                }
              });
      assertEquals("bad job", e.getMessage());
      assertEquals(200, e.getStatusCode().intValue());
    }
  }

  @Test
  void waitPollsToTerminal() throws Exception {
    server.enqueue(okJson("{\"output\":{\"job_id\":\"ft-7\",\"status\":\"RUNNING\"}}"));
    server.enqueue(
        okJson(
            "{\"output\":{\"job_id\":\"ft-7\",\"status\":\"SUCCEEDED\","
                + "\"finetuned_model\":\"m\"},\"request_id\":\"r7\"}"));
    FineTune job = client.fineTunes().wait("ft-7", RequestOptions.NONE);
    assertTrue(job.isSucceeded());
    assertEquals("m", job.getFinetunedModel());
    assertEquals(2, server.getRequestCount());
  }

  @Test
  void waitTimeoutBoundsAnInFlightPoll() {
    server.enqueue(
        okJson("{\"output\":{\"job_id\":\"ft-slow\",\"status\":\"RUNNING\"}}")
            .setBodyDelay(5, TimeUnit.SECONDS));
    long started = System.nanoTime();

    // The in-flight poll hits its budget-bounded call timeout; the reported failure must be a
    // wait timeout carrying the job id, not the context-free transport-level timeout message.
    ApiTimeoutException e =
        assertThrows(
            ApiTimeoutException.class,
            () -> client.fineTunes().wait("ft-slow", Duration.ofMillis(150), RequestOptions.NONE));
    assertTrue(e.getMessage().contains("ft-slow"), e.getMessage());

    long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
    assertTrue(elapsedMillis < 2000, "wait exceeded its total budget: " + elapsedMillis + "ms");
  }

  @Test
  void waitRaisesOnFailedJob() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"job_id\":\"ft-8\",\"status\":\"FAILED\",\"message\":\"boom\"},"
                + "\"request_id\":\"r8\"}"));
    ApiStatusException e =
        assertThrows(
            ApiStatusException.class, () -> client.fineTunes().wait("ft-8", RequestOptions.NONE));
    assertEquals("boom", e.getMessage());
    assertEquals(200, e.getStatusCode().intValue());
    assertEquals("r8", e.getRequestId());
  }

  @Test
  void waitReturnsErrorEnvelopeWhenNotRaising() throws Exception {
    try (DashScopeClient lenient =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .raiseOnError(false)
            .build()) {
      // A transport-level failure comes back as the error envelope instead of polling on.
      server.enqueue(
          new MockResponse()
              .setResponseCode(500)
              .setHeader("Content-Type", "application/json")
              .setBody("{\"code\":\"InternalError\",\"message\":\"x\",\"request_id\":\"r9\"}"));
      FineTune job = lenient.fineTunes().wait("ft-9", RequestOptions.NONE);
      assertTrue(job.isError());
      assertEquals("r9", job.getRequestId());
      assertEquals(1, server.getRequestCount());
    }
  }

  @Test
  void validationRejectsBadInput() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            FineTuneCreateParams.builder().trainingFileIds(Collections.singletonList("f")).build());
    assertThrows(
        IllegalArgumentException.class,
        () -> FineTuneCreateParams.builder().model("qwen-turbo").build());
    assertThrows(IllegalArgumentException.class, () -> client.fineTunes().get(""));
    assertThrows(
        IllegalArgumentException.class,
        () -> client.fineTunes().wait((String) null, RequestOptions.NONE));
  }

  @Test
  void asyncCreateCompletes() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"job_id\":\"ft-10\",\"status\":\"PENDING\"},\"request_id\":\"r10\"}"));
    try (com.alibaba.dashscope.DashScopeAsyncClient async =
        com.alibaba.dashscope.DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      FineTune job =
          async
              .fineTunes()
              .create(
                  FineTuneCreateParams.builder()
                      .model("qwen-turbo")
                      .trainingFileIds(Arrays.asList("f1", "f2"))
                      .build())
              .get(10, TimeUnit.SECONDS);
      assertEquals("ft-10", job.getJobId());
      assertEquals("r10", job.getRequestId());
    }
  }
}
