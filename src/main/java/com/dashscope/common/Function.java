package com.dashscope.common;

public enum Function {
  GENERATION("generation"),

  MULTIMODAL_CHAT("multimodal-chat"),
  ;

  private final String value;

  private Function(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
