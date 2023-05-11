package com.alibaba.dashscope.conversation;

public enum EventType {
  TASK_STARTED("task-started"),

  RESULT_GENERATED("result-generated"),

  TASK_FINISHED("task-finished"),

  TASK_FAILED("task-failed"),

  RUN_TASK("run-task"),
  ;

  private final String value;

  private EventType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
