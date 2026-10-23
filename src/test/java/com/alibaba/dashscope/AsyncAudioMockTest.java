// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.types.audio.AsrTranscribeParams;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** Async audio surface (audio design §1): async client carries asr batch transcription. */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class AsyncAudioMockTest {
  private MockWebServer server;
  private DashScopeAsyncClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client =
        DashScopeAsyncClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  @Test
  void asyncTranscribeCompletesWithTerminalJob() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"output\":{\"task_id\":\"tx-1\",\"task_status\":\"PENDING\"},"
                    + "\"request_id\":\"r-submit\"}"));
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"output\":{\"task_id\":\"tx-1\",\"task_status\":\"SUCCEEDED\"},"
                    + "\"request_id\":\"r-fetch\"}"));

    CompletableFuture<Job> future =
        client
            .audio()
            .asr()
            .transcribe(
                AsrTranscribeParams.builder()
                    .fileUrls(java.util.Arrays.asList("http://files/a.wav"))
                    .build());

    Job job = future.get(15, TimeUnit.SECONDS);
    assertEquals("tx-1", job.getTaskId());
    assertTrue(job.isSucceeded());

    RecordedRequest submit = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/audio/asr/transcription", submit.getPath());
    assertEquals("enable", submit.getHeader("X-DashScope-Async"));
    RecordedRequest fetch = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/tasks/tx-1", fetch.getPath());
  }

  @Test
  void asyncJobsExposeFetchAndCancel() throws Exception {
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"output\":{\"task_id\":\"tx-9\",\"task_status\":\"RUNNING\"},"
                    + "\"request_id\":\"r1\"}"));
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"output\":{\"task_id\":\"tx-9\",\"task_status\":\"CANCELED\"},"
                    + "\"request_id\":\"r2\"}"));

    Job running =
        client.audio().asr().jobs().fetch("tx-9", RequestOptions.NONE).get(15, TimeUnit.SECONDS);
    assertEquals("RUNNING", running.getTaskStatus());
    Job canceled =
        client.audio().asr().jobs().cancel("tx-9", RequestOptions.NONE).get(15, TimeUnit.SECONDS);
    assertEquals("CANCELED", canceled.getTaskStatus());

    RecordedRequest fetch = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("GET", fetch.getMethod());
    assertEquals("/tasks/tx-9", fetch.getPath());
    RecordedRequest cancel = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("POST", cancel.getMethod());
    assertEquals("/tasks/tx-9/cancel", cancel.getPath());
  }
}
