// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

public enum TaskStatus {
  PENDING("PENDING"),

  SUSPENDED("SUSPENDED"),

  SUCCEEDED("SUCCEEDED"),

  CANCELED("CANCELED"),

  RUNNING("RUNNING"),

  FAILED("FAILED"),

  UNKNOWN("UNKNOWN"),
  ;

  private final String value;

  private TaskStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
