// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

public enum Protocol {
  HTTP("http"),

  WEBSOCKET("websocket"),
  ;

  private final String value;

  private Protocol(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static Protocol of(String value) {
    for (Protocol protocol : Protocol.values()) {
      if (protocol.getValue().equals(value)) {
        return protocol;
      }
    }
    return null;
  }
}
