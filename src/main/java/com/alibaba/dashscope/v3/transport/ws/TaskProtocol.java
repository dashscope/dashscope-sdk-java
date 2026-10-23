// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.transport.ws;

import com.alibaba.dashscope.common.SdkErrorCode;
import com.alibaba.dashscope.v3.errors.Errors;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Bailian task-protocol framing (audio design §2): text frames {@code
 * {header:{action,task_id,streaming}, payload:{task_group,task,function,model,...}}} for
 * run/continue/finish-task; inbound events are task-started / result-generated / task-finished /
 * task-failed with {@code header.error_code/error_message} carrying failures.
 */
public final class TaskProtocol {
  public static final String RUN_TASK = "run-task";
  public static final String CONTINUE_TASK = "continue-task";
  public static final String FINISH_TASK = "finish-task";

  public static final String TASK_STARTED = "task-started";
  public static final String RESULT_GENERATED = "result-generated";
  public static final String TASK_FINISHED = "task-finished";
  public static final String TASK_FAILED = "task-failed";

  public static final String STREAMING_DUPLEX = "duplex";
  public static final String STREAMING_OUT = "out";
  public static final String STREAMING_NONE = "none";

  private TaskProtocol() {}

  /** task_id resolution (design: parameters.pre_task_id wins, else random UUID — 2.x parity). */
  public static String resolveTaskId(Map<String, ?> parameters) {
    if (parameters != null) {
      Object pre = parameters.get("pre_task_id");
      if (pre instanceof String && !((String) pre).isEmpty()) {
        return (String) pre;
      }
    }
    return UUID.randomUUID().toString();
  }

  /** A run/continue-task payload with the four envelope fields; caller fills input/parameters. */
  public static ObjectNode newPayload(
      String model, String taskGroup, String task, String function) {
    ObjectNode payload = JsonSupport.mapper().createObjectNode();
    if (model != null) {
      payload.put("model", model);
    }
    payload.put("task_group", taskGroup);
    payload.put("task", task);
    payload.put("function", function);
    return payload;
  }

  public static ObjectNode frame(String action, String taskId, String streaming, JsonNode payload) {
    ObjectNode header = JsonSupport.mapper().createObjectNode();
    header.put("action", action);
    header.put("task_id", taskId);
    if (streaming != null) {
      header.put("streaming", streaming);
    }
    ObjectNode root = JsonSupport.mapper().createObjectNode();
    root.set("header", header);
    if (payload != null) {
      root.set("payload", payload);
    }
    return root;
  }

  /** finish-task: {@code payload.input} is {@code {}} or {@code {"directive": <directive>}}. */
  public static ObjectNode finishTaskFrame(String taskId, String streaming, String directive) {
    ObjectNode input = JsonSupport.mapper().createObjectNode();
    if (directive != null && !directive.isEmpty()) {
      input.put("directive", directive);
    }
    ObjectNode payload = JsonSupport.mapper().createObjectNode();
    payload.set("input", input);
    return frame(FINISH_TASK, taskId, streaming, payload);
  }

  /** Parse one inbound text frame. Malformed frames raise {@code sdk.ws.ProtocolError}. */
  public static TaskEvent parse(String text) {
    JsonNode root;
    try {
      root = JsonSupport.mapper().readTree(text);
    } catch (IOException e) {
      throw protocolError("unparseable text frame: " + e.getMessage());
    }
    if (root == null || !root.isObject()) {
      throw protocolError("text frame is not a JSON object");
    }
    JsonNode header = root.get("header");
    if (header == null || !header.isObject()) {
      throw protocolError("frame has no header");
    }
    String taskId = textOrNull(header.get("task_id"));
    if (taskId == null) {
      throw protocolError("frame header has no task_id");
    }
    return TaskEvent.text(
        taskId,
        textOrNull(header.get("event")),
        textOrNull(header.get("error_code")),
        textOrNull(header.get("error_message")),
        root.get("payload"),
        root);
  }

  static com.alibaba.dashscope.v3.errors.SdkException protocolError(String detail) {
    return Errors.fromSdkErrorCode(
        SdkErrorCode.SDK_WS_PROTOCOL_ERROR, Collections.singletonMap("detail", detail));
  }

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
