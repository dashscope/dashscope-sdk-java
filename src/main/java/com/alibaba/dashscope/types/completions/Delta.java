// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.completions;

import com.alibaba.dashscope.types.messages.ToolCall;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Streaming delta inside a {@link ChatCompletionChunk} choice. */
public class Delta {
  @JsonProperty("role")
  private String role;

  @JsonProperty("content")
  private String content;

  @JsonProperty("reasoning_content")
  private String reasoningContent;

  @JsonProperty("reasoning_signature")
  private String reasoningSignature;

  @JsonProperty("tool_calls")
  private List<ToolCall> toolCalls;

  public String getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public String getReasoningContent() {
    return reasoningContent;
  }

  public String getReasoningSignature() {
    return reasoningSignature;
  }

  public List<ToolCall> getToolCalls() {
    return toolCalls;
  }
}
