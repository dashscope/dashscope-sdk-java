// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.common;

import com.google.gson.annotations.SerializedName;

@Deprecated
public enum Role {

  /** The human side. */
  @SerializedName("user")
  USER("user"),

  @SerializedName("assistant")
  ASSISTANT("assistant"),
  /** The model side. */
  @SerializedName("bot")
  BOT("bot"),

  @SerializedName("system")
  SYSTEM("system"),

  @SerializedName("attachment")
  ATTACHMENT("attachment"),

  @SerializedName("tool")
  TOOL("tool"),
  ;

  private final String value;

  private Role(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
