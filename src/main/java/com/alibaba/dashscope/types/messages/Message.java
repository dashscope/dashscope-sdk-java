// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.messages;

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

  @JsonProperty("tool_call_id")
  private final String toolCallId;

  /** Retained for source compatibility with the original 3.0 message constructor. */
  public Message(String role, String content, String name, List<ToolCall> toolCalls) {
    this(role, content, name, toolCalls, null);
  }

  public Message(
      String role, String content, String name, List<ToolCall> toolCalls, String toolCallId) {
    this.role = role;
    this.content = content;
    this.name = name;
    this.toolCalls = toolCalls;
    this.toolCallId = toolCallId;
  }

  public static Message of(String role, String content) {
    return new Message(role, content, null, null, null);
  }

  public static Message user(String content) {
    return new Message("user", content, null, null, null);
  }

  public static Message system(String content) {
    return new Message("system", content, null, null, null);
  }

  public static Message assistant(String content) {
    return new Message("assistant", content, null, null, null);
  }

  public static Message assistant(String content, List<ToolCall> toolCalls) {
    return new Message("assistant", content, null, toolCalls, null);
  }

  /** Legacy name-only tool result form. Prefer {@link #tool(String, String, String)}. */
  public static Message tool(String content, String name) {
    return new Message("tool", content, name, null, null);
  }

  /** Tool result associated with the model-issued {@code tool_call_id}. */
  public static Message tool(String content, String name, String toolCallId) {
    return new Message("tool", content, name, null, toolCallId);
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

  public String getToolCallId() {
    return toolCallId;
  }
}
