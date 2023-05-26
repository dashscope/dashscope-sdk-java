// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum WebSocketEventType {
  TASK_STARTED("task-started"),

  RESULT_GENERATED("result-generated"),

  TASK_FINISHED("task-finished"),

  FINISH_TASK("finish-task"),

  TASK_FAILED("task-failed"),

  RUN_TASK("run-task"),

  CONTINUE_TASK("continue-task"),
  ;

  private final String value;

  private WebSocketEventType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
