// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A tool call issued by the model: function name plus JSON-encoded arguments. */
public final class ToolCall {
  @JsonProperty("id")
  private String id;

  @JsonProperty("type")
  private String type;

  @JsonProperty("function")
  private ToolCallFunction function;

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public ToolCallFunction getFunction() {
    return function;
  }
}
