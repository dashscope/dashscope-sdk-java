// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.protocol;

/** 3.0 copy of the 2.x Protocol for the agentstudio subtree. */
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
    for (Protocol p : Protocol.values()) {
      if (p.value.equals(value)) {
        return p;
      }
    }
    return null;
  }
}
