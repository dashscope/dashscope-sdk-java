package com.alibaba.dashscope.common;

public enum Role {

  /** The human side. */
  USER("user"),

  /** The model side. */
  BOT("bot"),
  ;

  private final String value;

  private Role(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
