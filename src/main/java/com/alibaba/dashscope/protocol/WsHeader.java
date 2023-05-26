// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import static com.alibaba.dashscope.utils.ApiKeywords.ERROR_MESSAGE;
import static com.alibaba.dashscope.utils.ApiKeywords.ERROR_NAME;

import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.common.WebSocketEventType;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WsHeader {

  /** The task id */
  @SerializedName("task_id")
  private String taskId;

  /**
   * Indicate whether this task is a streaming inference task (input only). none: Non-streaming
   * inference task n: Streaming input, non-streaming output in: Non-streaming input, streaming
   * output out: Streaming input, non-streaming output duplex: Streaming input, streaming output
   */
  private String streaming;

  /**
   * Model input message type (input only). run-task: First input message of an inference task
   * finish-task: Last input message of an inference task (if the inference task is streaming)
   * continue-task: Intermediate streaming input message of an inference task (if the inference task
   * is streaming)
   */
  private String action;

  /**
   * Model output events (output only). task-started: Model has started inference, this message is
   * for notification only and does not carry any inference results task-finished: Inference task
   * has finished, this message carries the final inference result result-generated: Streaming
   * result of an inference task (if the inference task is streaming) task-failed: Inference task
   * has failed
   */
  private String event;

  /** The error name. */
  @SerializedName(ERROR_NAME)
  private String errorName;

  /** The error message. */
  @SerializedName(ERROR_MESSAGE)
  private String errorMessage;

  public static WsHeader buildInputHeader(
      String taskId, StreamingMode mode, WebSocketEventType eventType) {
    WsHeader header = new WsHeader();
    header.setTaskId(taskId);
    header.setStreaming(mode.getValue());
    header.setAction(eventType.getValue());
    return header;
  }
}
