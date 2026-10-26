// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.jobs;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.common.Page;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic async-task service (design §3): submit with {@code X-DashScope-Async: enable}, poll
 * {@code GET /tasks/{taskId}}, cancel via {@code POST /tasks/{taskId}/cancel}, list via {@code GET
 * /tasks}. {@code wait} polls with a 1s → 8s backoff and a 24h default budget.
 */
public final class JobsService {
  static final String HEADER_ASYNC = "X-DashScope-Async";
  static final String TASKS_PATH = "/tasks";

  /** Upper bound on the total wait budget (design §3). */
  public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofHours(24);

  private static final long INITIAL_POLL_MS = 1000;
  private static final long MAX_POLL_MS = 8000;

  private final HttpTransport transport;
  private final String module;

  public JobsService(HttpTransport transport, String module) {
    this.transport = transport;
    this.module = module;
  }

  /** Submit an async job: POST {@code servicePath} with the async header. */
  public Job create(String servicePath, JsonNode body, RequestOptions options) {
    return create(servicePath, body, null, options);
  }

  /** Submit an async job carrying extra request headers (e.g. the OSS resolve opt-in). */
  public Job create(
      String servicePath, JsonNode body, Map<String, String> extraHeaders, RequestOptions options) {
    Map<String, String> headers = new LinkedHashMap<>();
    if (extraHeaders != null) {
      headers.putAll(extraHeaders);
    }
    headers.put(HEADER_ASYNC, "enable");
    return toJob(transport.execute(servicePath, body, headers, module, options));
  }

  public Job fetch(String taskId, RequestOptions options) {
    return toJob(transport.executeGet(TASKS_PATH + "/" + taskId, null, module, options));
  }

  /**
   * Blocks until a terminal state; equivalent to repeatedly {@code jobs().fetch} with a 1s→8s
   * backoff (default budget 24h).
   */
  public Job wait(String taskId, RequestOptions options) {
    return wait(taskId, DEFAULT_WAIT_TIMEOUT, options);
  }

  public Job wait(String taskId, Duration waitTimeout, RequestOptions options) {
    long timeoutMillis =
        waitTimeout == null ? DEFAULT_WAIT_TIMEOUT.toMillis() : waitTimeout.toMillis();
    long start = System.currentTimeMillis();
    long pollMs = INITIAL_POLL_MS;
    while (true) {
      Job job = fetch(taskId, options);
      if (job.isFinished()) {
        return job;
      }
      long elapsed = System.currentTimeMillis() - start;
      if (elapsed >= timeoutMillis) {
        throw Errors.timeout(
            SdkErrorCode.SDK_TIMEOUT,
            "waiting for task [" + taskId + "] after " + elapsed + " ms",
            null);
      }
      long sleepMs = Math.min(pollMs, timeoutMillis - elapsed);
      pollMs = Math.min(pollMs * 2, MAX_POLL_MS);
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "waiting for task [" + taskId + "]", e);
      }
    }
  }

  public Job cancel(String taskId, RequestOptions options) {
    return toJob(transport.execute(TASKS_PATH + "/" + taskId + "/cancel", null, module, options));
  }

  /** List jobs; query keys follow the wire names (page_no/page_size/model_name/status/...). */
  public Page<Job> list(Map<String, String> query, RequestOptions options) {
    ResponseEnvelope envelope = transport.executeGet(TASKS_PATH, query, module, options);
    JsonNode output = envelope.getBody() == null ? null : envelope.getBody().get("output");
    List<Job> items = new ArrayList<>();
    int totalCount = 0;
    int pageNo = 0;
    int pageSize = 0;
    if (output != null) {
      JsonNode data = output.get("data");
      if (data != null && data.isArray()) {
        for (JsonNode item : data) {
          items.add(Job.fromOutputNode(item));
        }
      }
      totalCount = intOrZero(output.get("total"));
      pageNo = intOrZero(output.get("page_no"));
      pageSize = intOrZero(output.get("page_size"));
    }
    Page<Job> page = new Page<>(items, totalCount, pageNo, pageSize);
    page.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return page;
  }

  private static int intOrZero(JsonNode node) {
    return node != null && node.isNumber() ? node.asInt() : 0;
  }

  private Job toJob(ResponseEnvelope envelope) {
    Job job = Job.fromBody(envelope.getBody());
    job.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return job;
  }
}
