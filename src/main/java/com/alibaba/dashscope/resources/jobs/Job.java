// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.jobs;

import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.common.ResponseBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A long-running task (design §3): {@code taskId} is the primary name ({@code job_id} accepted as
 * an alias), {@code outputAs(Class)} gives a typed view over the raw output node. Terminal states:
 * SUCCEEDED / FAILED / CANCELED / UNKNOWN.
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

  /**
   * Build from the task envelope body {@code {request_id, output:{task_id,task_status,...}}}.
   * SDK-internal wiring (transport → job); users must not call it.
   */
  public static Job fromBody(JsonNode body) {
    Job job = new Job();
    JsonNode out = body == null ? null : body.get("output");
    job.usage = body == null ? null : body.get("usage");
    fillFromOutput(job, out);
    return job;
  }

  /**
   * Build from a bare output node (a {@code data[]} item of the list endpoint, where the status
   * field is named {@code status} rather than {@code task_status}).
   */
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
      if (job.taskStatus == null) {
        // GET /tasks list items carry "status" instead of "task_status" (2.x AsyncTaskInfo).
        job.taskStatus = textOrNull(out.get("status"));
      }
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

  // Preserve existing empty-versus-populated collection mutability.
  /**
   * The artifacts of a finished task (Python v2 {@code Job.results} parity): {@code output.results}
   * when present, else the image parts lifted from a choices-style output (multimodal-generation
   * answers image tasks with chat-style choices).
   */
  @SuppressWarnings("MixedMutabilityReturnType")
  public List<JobResult> getResults() {
    if (output == null || !output.isObject()) {
      return Collections.emptyList();
    }
    JsonNode results = output.get("results");
    if (results == null || !results.isArray() || results.isEmpty()) {
      results = imageChoices(output);
    }
    if (results == null || !results.isArray()) {
      return Collections.emptyList();
    }
    List<JobResult> out = new ArrayList<>();
    for (JsonNode item : results) {
      if (item != null && item.isObject()) {
        out.add(JsonSupport.mapper().convertValue(item, JobResult.class));
      }
    }
    return out;
  }

  /** Image URLs of a finished image task; empty for other tasks. */
  public List<String> getImageUrls() {
    List<String> out = new ArrayList<>();
    for (JobResult result : getResults()) {
      if (result.getUrl() != null) {
        out.add(result.getUrl());
      }
    }
    return out;
  }

  /** Video URL of a finished video task; null for other tasks. */
  public String getVideoUrl() {
    if (output == null || !output.isObject()) {
      return null;
    }
    JsonNode url = output.get("video_url");
    return url != null && url.isTextual() ? url.asText() : null;
  }

  /** The single artifact of a one-result task: first image URL, else output.url, else video. */
  public String getResultUrl() {
    List<String> urls = getImageUrls();
    if (!urls.isEmpty()) {
      return urls.get(0);
    }
    if (output != null && output.isObject()) {
      JsonNode url = output.get("url");
      if (url != null && url.isTextual()) {
        return url.asText();
      }
    }
    return getVideoUrl();
  }

  /** Synthesize results entries from a choices-style image output (multimodal-generation). */
  private static JsonNode imageChoices(JsonNode output) {
    JsonNode choices = output.get("choices");
    if (choices == null || !choices.isArray()) {
      return null;
    }
    com.fasterxml.jackson.databind.node.ArrayNode lifted = JsonSupport.mapper().createArrayNode();
    for (JsonNode choice : choices) {
      if (choice == null || !choice.isObject()) {
        continue;
      }
      JsonNode message = choice.get("message");
      JsonNode content = message == null ? null : message.get("content");
      if (content == null || !content.isArray()) {
        continue;
      }
      for (JsonNode part : content) {
        if (part != null && part.isObject() && part.hasNonNull("image")) {
          com.fasterxml.jackson.databind.node.ObjectNode entry = lifted.addObject();
          entry.set("url", part.get("image"));
        }
      }
    }
    return lifted.isEmpty() ? null : lifted;
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
