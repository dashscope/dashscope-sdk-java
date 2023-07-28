// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.google.gson.annotations.SerializedName;

public enum WebSocketEventType {
  // receive
  @SerializedName("task-started")
  TASK_STARTED("task-started"),

  @SerializedName("result-generated")
  RESULT_GENERATED("result-generated"),

  @SerializedName("task-finished")
  TASK_FINISHED("task-finished"),

  @SerializedName("task-failed")
  TASK_FAILED("task-failed"),

  // send
  @SerializedName("run-task")
  RUN_TASK("run-task"),

  @SerializedName("continue-task")
  CONTINUE_TASK("continue-task"),

  @SerializedName("finish-task")
  FINISH_TASK("finish-task"),
  ;

  private final String value;

  private WebSocketEventType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
