package com.dashscope.conversation;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class ConversationMessageHeader {

  /** The task id */
  @JSONField(name = "task_id")
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
  @JSONField(name = "error_name")
  private String errorName;

  /** The error message. */
  @JSONField(name = "error_message")
  private String errorMessage;

  public static ConversationMessageHeader buildInputHeader(String taskId, boolean streaming) {
    ConversationMessageHeader header = new ConversationMessageHeader();
    header.setTaskId(taskId);
    header.setStreaming(streaming ? "out" : "none");
    header.setAction(EventType.RUN_TASK.getValue());
    return header;
  }
}
