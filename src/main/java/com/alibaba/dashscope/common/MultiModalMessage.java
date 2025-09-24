// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import java.util.List;
import java.util.Map;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class MultiModalMessage {

  /** The role, can be `user` and `bot`. */
  private String role;

  /** The conversation content. */
  // TODO maybe a abstract or interface for content instead of map, now not user friendly for
  // developers
  private List<Map<String, Object>> content;

  /** For tool calls */
  @SerializedName("tool_calls")
  List<ToolCallBase> toolCalls;

  /** For tool result */
  @SerializedName("tool_call_id")
  String toolCallId;

  /** tool name */
  @SerializedName("name")
  String name;

  /** chain of thought content */
  @SerializedName("reasoning_content")
  String reasoningContent;

  /** annotations result for message */
  private List<Map<String, Object>> annotations;
}
