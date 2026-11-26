// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.finetunes;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.common.JobStatus;
import com.alibaba.dashscope.types.finetunes.FineTune;
import com.alibaba.dashscope.types.finetunes.FineTuneCreateParams;
import com.alibaba.dashscope.types.finetunes.FineTuneList;
import com.alibaba.dashscope.types.finetunes.JobLogs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code fineTunes} resource (design §3, Python v2 fine_tunes.py parity): create/get/list/cancel/
 * delete over {@code /fine-tunes}, plus training {@code logs}, the {@code streamEvents} SSE feed,
 * and {@code wait} — a poller over {@code get} (this plane has no {@code /tasks} entry; UNKNOWN is
 * deliberately not terminal here, unlike {@code Job}).
 */
public final class FineTunesService {
  static final String BASE_PATH = "/fine-tunes";
  static final String MODULE = "finetune";

  public static final Duration DEFAULT_WAIT_TIMEOUT = Duration.ofHours(24);
  private static final long INITIAL_POLL_MS = 1000;
  private static final long MAX_POLL_MS = 8000;

  private final HttpTransport transport;

  public FineTunesService(HttpTransport transport) {
    this.transport = transport;
  }

  public FineTune create(FineTuneCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public FineTune create(FineTuneCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope = transport.execute(BASE_PATH, buildBody(params), MODULE, options);
    return toFineTune(envelope);
  }

  public FineTune get(String jobId) {
    return get(jobId, RequestOptions.NONE);
  }

  public FineTune get(String jobId, RequestOptions options) {
    requireJobId(jobId);
    ResponseEnvelope envelope =
        transport.executeGet(BASE_PATH + "/" + jobId, null, MODULE, options);
    return toFineTune(envelope);
  }

  public FineTuneList list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public FineTuneList list(int pageNo, int pageSize, RequestOptions options) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page_no", String.valueOf(pageNo));
    query.put("page_size", String.valueOf(pageSize));
    ResponseEnvelope envelope = transport.executeGet(BASE_PATH, query, MODULE, options);
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    List<FineTune> jobs = new ArrayList<>();
    JsonNode jobsNode = flat.get("jobs");
    if (jobsNode != null && jobsNode.isArray()) {
      for (JsonNode item : jobsNode) {
        if (item != null && item.isObject()) {
          FineTunesSupport.normalizeUsage((ObjectNode) item);
        }
        FineTune job = JsonSupport.mapper().convertValue(item, FineTune.class);
        // A failed job in a list flags itself through isError(): the server reports a failure
        // code per job, so the item's own code/message fill its envelope.
        job.fillEnvelope(
            textOrNull(item.get("request_id")),
            envelope.getStatusCode(),
            textOrNull(item.get("code")),
            textOrNull(item.get("message")),
            envelope.getHeaders());
        jobs.add(job);
      }
    }
    FineTuneList result =
        new FineTuneList(
            intOr(flat.get("page_no"), 1),
            intOr(flat.get("page_size"), 10),
            intOr(flat.get("total"), jobs.size()),
            jobs);
    fillFromFlat(result, flat, envelope);
    return result;
  }

  public JobStatus cancel(String jobId) {
    return cancel(jobId, RequestOptions.NONE);
  }

  public JobStatus cancel(String jobId, RequestOptions options) {
    requireJobId(jobId);
    ResponseEnvelope envelope =
        transport.execute(BASE_PATH + "/" + jobId + "/cancel", null, MODULE, options);
    return toJobStatus(envelope);
  }

  public JobStatus delete(String jobId) {
    return delete(jobId, RequestOptions.NONE);
  }

  public JobStatus delete(String jobId, RequestOptions options) {
    requireJobId(jobId);
    ResponseEnvelope envelope = transport.executeDelete(BASE_PATH + "/" + jobId, MODULE, options);
    return toJobStatus(envelope);
  }

  public JobLogs logs(String jobId, int offset, int lines) {
    return logs(jobId, offset, lines, RequestOptions.NONE);
  }

  public JobLogs logs(String jobId, int offset, int lines, RequestOptions options) {
    requireJobId(jobId);
    Map<String, String> query = new LinkedHashMap<>();
    query.put("offset", String.valueOf(offset));
    // The wire parameter is singular "line".
    query.put("line", String.valueOf(lines));
    ResponseEnvelope envelope =
        transport.executeGet(BASE_PATH + "/" + jobId + "/logs", query, MODULE, options);
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    JobLogs result = new JobLogs(jobId, logsText(flat.get("logs")));
    fillFromFlat(result, flat, envelope);
    return result;
  }

  /** The job-event SSE feed. Close the returned stream (try-with-resources). */
  public FineTuneEventStream streamEvents(String jobId) {
    return streamEvents(jobId, RequestOptions.NONE);
  }

  public FineTuneEventStream streamEvents(String jobId, RequestOptions options) {
    requireJobId(jobId);
    return new FineTuneEventStream(
        transport.executeJobEventStream(BASE_PATH + "/" + jobId + "/stream", MODULE, options),
        transport.config().isRaiseOnError());
  }

  public FineTune wait(String jobId, RequestOptions options) {
    return wait(jobId, DEFAULT_WAIT_TIMEOUT, options);
  }

  /** Poll {@code get} with a 1s → 8s backoff until the job reaches a terminal status. */
  public FineTune wait(String jobId, Duration waitTimeout, RequestOptions options) {
    requireJobId(jobId);
    return waitLoop(jobId, null, waitTimeout, options);
  }

  /** Same wait, starting from an already-fetched job snapshot (saves one round trip). */
  public FineTune wait(FineTune job, Duration waitTimeout, RequestOptions options) {
    if (job == null) {
      throw new IllegalArgumentException("fine-tune job required");
    }
    return waitLoop(requireJobId(job.getJobId()), job, waitTimeout, options);
  }

  private FineTune waitLoop(
      String jobId, FineTune current, Duration waitTimeout, RequestOptions options) {
    Duration budget = waitTimeout == null ? DEFAULT_WAIT_TIMEOUT : waitTimeout;
    long timeoutNanos = budget.toNanos();
    long start = System.nanoTime();
    long pollMs = INITIAL_POLL_MS;
    if (current == null) {
      long remainingNanos = timeoutNanos - (System.nanoTime() - start);
      if (remainingNanos <= 0) {
        throw waitTimeout(jobId, budget);
      }
      current = poll(jobId, options, timeoutNanos, start, budget);
    }
    while (!current.isFinished()) {
      // Compat mode (raiseOnError=false): a transport-level failure comes back as an error
      // envelope with no job status, which would otherwise poll until the timeout.
      if (current.isError()) {
        return current;
      }
      long elapsedNanos = System.nanoTime() - start;
      long remainingNanos = timeoutNanos - elapsedNanos;
      if (remainingNanos <= 0) {
        throw waitTimeout(jobId, budget);
      }
      long sleepMs = Math.min(pollMs, Duration.ofNanos(remainingNanos).toMillis());
      pollMs = Math.min(pollMs * 2, MAX_POLL_MS);
      try {
        Thread.sleep(sleepMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw Errors.connection(
            SdkErrorCode.SDK_CONNECTION_INTERRUPTED, "waiting for fine-tune job " + jobId, e);
      }
      remainingNanos = timeoutNanos - (System.nanoTime() - start);
      if (remainingNanos <= 0) {
        throw waitTimeout(jobId, budget);
      }
      current = poll(jobId, options, timeoutNanos, start, budget);
    }
    return checkTerminal(current, jobId);
  }

  /**
   * A single poll bounded by the remaining budget. A budget-bounded poll that times out is
   * reported as a wait timeout carrying the job id (the transport-level message has no job
   * context); a caller-configured shorter per-call timeout propagates unchanged.
   */
  private FineTune poll(
      String jobId, RequestOptions options, long timeoutNanos, long start, Duration budget) {
    long remainingNanos = timeoutNanos - (System.nanoTime() - start);
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    Duration configured =
        opts.getTimeout() != null ? opts.getTimeout() : transport.config().getTimeout();
    // OkHttp truncates Duration timeouts to whole milliseconds, so a budget-bounded call can
    // fire a hair before the budget clock reads exhausted; classify by which timeout was
    // applied, not by re-reading the clock in the catch.
    boolean budgetBounded = Duration.ofNanos(remainingNanos).compareTo(configured) < 0;
    try {
      return get(jobId, boundedOptions(opts, remainingNanos));
    } catch (ApiTimeoutException e) {
      if (budgetBounded || timeoutNanos - (System.nanoTime() - start) <= 0) {
        throw waitTimeout(jobId, budget, e);
      }
      throw e;
    }
  }

  private static RuntimeException waitTimeout(String jobId, Duration budget) {
    return waitTimeout(jobId, budget, null);
  }

  private static RuntimeException waitTimeout(String jobId, Duration budget, Throwable cause) {
    return Errors.timeout(
        SdkErrorCode.SDK_TIMEOUT,
        "waiting for fine-tune job " + jobId + " after " + budget.toMillis() + "ms",
        cause);
  }

  private RequestOptions boundedOptions(RequestOptions options, long remainingNanos) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    Duration remaining = Duration.ofNanos(Math.max(1_000_000L, remainingNanos));
    Duration configured =
        opts.getTimeout() != null ? opts.getTimeout() : transport.config().getTimeout();
    Duration bounded = configured.compareTo(remaining) <= 0 ? configured : remaining;
    return opts.toBuilder().timeout(bounded).build();
  }

  private FineTune checkTerminal(FineTune job, String jobId) {
    if (!transport.config().isRaiseOnError()) {
      return job;
    }
    if (FineTune.FAILED.equals(job.getStatus())) {
      throw Errors.fromBusinessCode(
          job.getCode(),
          job.getMessage() != null ? job.getMessage() : "fine-tune job " + jobId + " failed",
          job.getRequestId(),
          null,
          job.getHeaders());
    }
    // CANCELED is a caller-initiated terminal state, not a failure: hand the job back.
    return job;
  }

  private static ObjectNode buildBody(FineTuneCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    root.putPOJO("training_file_ids", params.getTrainingFileIds());
    if (params.getValidationFileIds() != null) {
      root.putPOJO("validation_file_ids", params.getValidationFileIds());
    } else {
      root.putNull("validation_file_ids");
    }
    ObjectNode hyper = root.putObject("hyper_parameters");
    if (params.getHyperParameters() != null) {
      params.getHyperParameters().forEach((k, v) -> hyper.set(k, mapper.valueToTree(v)));
    }
    if (params.getMode() != null) {
      root.put("training_type", params.getMode());
    }
    if (params.getFinetunedModel() != null) {
      root.put("model_name", params.getFinetunedModel());
    }
    return root;
  }

  private static FineTune toFineTune(ResponseEnvelope envelope) {
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    FineTunesSupport.normalizeUsage(flat);
    FineTune job = JsonSupport.mapper().convertValue(flat, FineTune.class);
    // The flattened node is the envelope source (Python flatten_envelope parity): a failed
    // job's code/message live inside output and must reach isError()/getMessage().
    fillFromFlat(job, flat, envelope);
    return job;
  }

  private static JobStatus toJobStatus(ResponseEnvelope envelope) {
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    JobStatus status = JsonSupport.mapper().convertValue(flat, JobStatus.class);
    fillFromFlat(status, flat, envelope);
    return status;
  }

  private static void fillFromFlat(
      com.alibaba.dashscope.types.common.ResponseBase result,
      ObjectNode flat,
      ResponseEnvelope envelope) {
    String requestId = textOrNull(flat.get("request_id"));
    if (requestId == null) {
      requestId = JsonSupport.requestId(null, envelope.getHeaders());
    }
    result.fillEnvelope(
        requestId,
        envelope.getStatusCode(),
        textOrNull(flat.get("code")),
        textOrNull(flat.get("message")),
        envelope.getHeaders());
  }

  private static String logsText(JsonNode logs) {
    if (logs == null || logs.isNull()) {
      return "";
    }
    if (logs.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode line : logs) {
        if (sb.length() > 0) {
          sb.append('\n');
        }
        sb.append(line == null || line.isNull() ? "" : line.asText());
      }
      return sb.toString();
    }
    return logs.asText();
  }

  private static int intOr(JsonNode node, int fallback) {
    return node != null && node.isNumber() ? node.asInt() : fallback;
  }

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }

  private static String requireJobId(String jobId) {
    if (jobId == null || jobId.isEmpty()) {
      throw new IllegalArgumentException("fine-tune job id required");
    }
    return jobId;
  }
}
