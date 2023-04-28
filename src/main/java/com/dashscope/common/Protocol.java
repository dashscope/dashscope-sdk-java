package com.dashscope.common;

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
}
