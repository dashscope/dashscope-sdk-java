// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** One chat message. Static factories stand in for Python's module-level constructors. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Message {
  @JsonProperty("role")
  private final String role;

  @JsonProperty("content")
  private final String content;

  @JsonProperty("name")
  private final String name;

  @JsonProperty("tool_calls")
  private final List<ToolCall> toolCalls;

  public Message(String role, String content, String name, List<ToolCall> toolCalls) {
    this.role = role;
    this.content = content;
    this.name = name;
    this.toolCalls = toolCalls;
  }

  public static Message of(String role, String content) {
    return new Message(role, content, null, null);
  }

  public static Message user(String content) {
    return new Message("user", content, null, null);
  }

  public static Message system(String content) {
    return new Message("system", content, null, null);
  }

  public static Message assistant(String content) {
    return new Message("assistant", content, null, null);
  }

  public static Message assistant(String content, List<ToolCall> toolCalls) {
    return new Message("assistant", content, null, toolCalls);
  }

  public static Message tool(String content, String name) {
    return new Message("tool", content, name, null);
  }

  public String getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public String getName() {
    return name;
  }

  public List<ToolCall> getToolCalls() {
    return toolCalls;
  }
}
