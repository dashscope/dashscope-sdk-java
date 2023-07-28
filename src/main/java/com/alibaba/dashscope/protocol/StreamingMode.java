// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

public enum StreamingMode {

  /** one message in, one message out. */
  NONE("none"),

  /** Stream in, one message out. */
  IN("in"),

  /** one message in, stream out. */
  OUT("out"),

  /** stream in, stream out. */
  DUPLEX("duplex"),
  ;

  private final String value;

  private StreamingMode(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static StreamingMode of(String value) {
    for (StreamingMode mode : StreamingMode.values()) {
      if (mode.value.equals(value)) {
        return mode;
      }
    }
    return null;
  }
}
