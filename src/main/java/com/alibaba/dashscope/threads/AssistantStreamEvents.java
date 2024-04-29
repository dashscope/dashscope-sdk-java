package com.alibaba.dashscope.threads;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public enum AssistantStreamEvents {
  @SerializedName("thread.created")
  THREAD_CREATED("thread.created"),
  @SerializedName("thread.run.created")
  THREAD_RUN_CREATED("thread.run.created"),
  @SerializedName("thread.run.queued")
  THREAD_RUN_QUEUED("thread.run.queued"),
  @SerializedName("thread.run.in_progress")
  THREAD_RUN_IN_PROGRESS("thread.run.in_progress"),
  @SerializedName("thread.run.requires_action")
  THREAD_RUN_REQUIRES_ACTION("thread.run.requires_action"),
  @SerializedName("thread.run.completed")
  THREAD_RUN_COMPLETED("thread.run.completed"),
  @SerializedName("thread.run.failed")
  THREAD_RUN_FAILED("thread.run.failed"),
  @SerializedName("thread.run.cancelling")
  THREAD_RUN_CANCELLING("thread.run.cancelling"),
  @SerializedName("thread.run.cancelled")
  THREAD_RUN_CANCELED("thread.run.cancelled"),
  @SerializedName("thread.run.expired")
  THREAD_RUN_EXPIRED("thread.run.expired"),
  @SerializedName("thread.run.step.created")
  THREAD_RUN_STEP_CREATED("thread.run.step.created"),
  @SerializedName("thread.run.step.in_progress")
  THREAD_RUN_STEP_IN_PROGRESS("thread.run.step.in_progress"),
  @SerializedName("thread.run.step.delta")
  THREAD_RUN_STEP_DELTA("thread.run.step.delta"),
  @SerializedName("thread.run.step.completed")
  THREAD_RUN_STEP_COMPLETED("thread.run.step.completed"),
  @SerializedName("thread.run.step.failed")
  THREAD_RUN_STEP_FAILED("thread.run.step.failed"),
  @SerializedName("thread.run.step.cancelled")
  THREAD_RUN_STEP_CANCELLED("thread.run.step.cancelled"),
  @SerializedName("thread.run.step.expired")
  THREAD_RUN_STEP_EXPIRED("thread.run.step.expired"),
  @SerializedName("thread.message.created")
  THREAD_MESSAGE_CREATED("thread.message.created"),
  @SerializedName("thread.message.in_progress")
  THREAD_MESSAGE_IN_PROGRESS("thread.message.in_progress"),
  @SerializedName("thread.message.delta")
  THREAD_MESSAGE_DELTA("thread.message.delta"),
  @SerializedName("thread.message.completed")
  THREAD_MESSAGE_COMPLETED("thread.message.completed"),
  @SerializedName("thread.message.incomplete")
  THREAD_MESSAGE_INCOMPLETE("thread.message.incomplete"),
  @SerializedName("error")
  ERROR("error"),
  @SerializedName("done")
  DONE("done"),
  @SerializedName("unknown")
  UNKNOWN("unknown");
  private final String value;
  private static final Map<String, AssistantStreamEvents> CONSTANTS = new HashMap<>();

  static {
    for (AssistantStreamEvents c : values()) {
      CONSTANTS.put(c.value, c);
    }
  }

  AssistantStreamEvents(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String value() {
    return this.value;
  }

  public static AssistantStreamEvents fromValue(String value) {
    AssistantStreamEvents constant = CONSTANTS.get(value);
    if (constant == null) {
      return UNKNOWN;
    } else {
      return constant;
    }
  }
}
