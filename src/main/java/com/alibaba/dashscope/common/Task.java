// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum Task {
  TEXT_GENERATION("text-generation"),

  CHAT("chat"),
  ;

  private final String value;

  private Task(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
