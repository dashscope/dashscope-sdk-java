// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.jobs;

import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.common.ResponseBase;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * A long-running task (design §3): {@code taskId} is the primary name ({@code job_id} accepted
 * as an alias), {@code outputAs(Class)} gives a typed view over the raw output node. Terminal
 * states: SUCCEEDED / FAILED / CANCELED / UNKNOWN.
 */
public final class Job extends ResponseBase {
  public static final String PENDING = "PENDING";
  public static final String RUNNING = "RUNNING";
  public static final String SUCCEEDED = "SUCCEEDED";
  public static final String FAILED = "FAILED";
  public static final String CANCELED = "CANCELED";
  public static final String UNKNOWN = "UNKNOWN";

  private String taskId;
  private String taskStatus;
  private JsonNode output;
  private JsonNode usage;

  /** Build from the task envelope body {@code {request_id, output:{task_id,task_status,...}}}. */
  static Job fromBody(JsonNode body) {
    Job job = new Job();
    JsonNode out = body == null ? null : body.get("output");
    job.usage = body == null ? null : body.get("usage");
    fillFromOutput(job, out);
    return job;
  }

  /** Build from a bare output node (a {@code data[]} item of the list endpoint). */
  static Job fromOutputNode(JsonNode output) {
    Job job = new Job();
    fillFromOutput(job, output);
    return job;
  }

  private static void fillFromOutput(Job job, JsonNode out) {
    job.output = out;
    if (out != null) {
      job.taskId = textOrNull(out.get("task_id"));
      if (job.taskId == null) {
        job.taskId = textOrNull(out.get("job_id"));
      }
      job.taskStatus = textOrNull(out.get("task_status"));
    }
  }

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getTaskStatus() {
    return taskStatus;
  }

  /** Raw output node of the task. */
  public JsonNode getOutput() {
    return output;
  }

  public JsonNode getUsage() {
    return usage;
  }

  /** Typed view over the raw output node. */
  public <T> T outputAs(Class<T> type) {
    return JsonSupport.mapper().convertValue(output, type);
  }

  public boolean isSucceeded() {
    return SUCCEEDED.equals(taskStatus);
  }

  /** Terminal: SUCCEEDED / FAILED / CANCELED / UNKNOWN (2.x TaskStatus parity). */
  public boolean isFinished() {
    return SUCCEEDED.equals(taskStatus)
        || FAILED.equals(taskStatus)
        || CANCELED.equals(taskStatus)
        || UNKNOWN.equals(taskStatus);
  }
}
