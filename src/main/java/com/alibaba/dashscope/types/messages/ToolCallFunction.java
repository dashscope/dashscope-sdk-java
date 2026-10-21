// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Function payload of a {@link ToolCall}; {@code arguments} is the JSON-encoded argument bag. */
public final class ToolCallFunction {
  @JsonProperty("name")
  private String name;

  @JsonProperty("arguments")
  private String arguments;

  public String getName() {
    return name;
  }

  public String getArguments() {
    return arguments;
  }
}
