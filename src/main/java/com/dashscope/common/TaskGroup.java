package com.dashscope.common;

public enum TaskGroup {
  AIGC("aigc"),
  ;

  private final String value;

  private TaskGroup(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
