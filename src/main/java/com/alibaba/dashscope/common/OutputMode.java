// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

public enum OutputMode {
  ACCUMULATE("accumulate"),
  DIVIDE("divide"),
  ;

  private final String value;

  private OutputMode(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static OutputMode of(String value) {
    for (OutputMode mode : OutputMode.values()) {
      if (mode.value.equals(value)) {
        return mode;
      }
    }
    return null;
  }
}
