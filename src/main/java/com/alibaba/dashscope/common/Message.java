// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/** represent input and output messages. */
@Data
@SuperBuilder
@NoArgsConstructor
public class Message {

  /** The role, can be `user` and `system` and `assistant` and 'tool'. */
  String role;

  /** The conversation content. */
  String content;

  /** For tool calls */
  @SerializedName("tool_calls")
  List<ToolCallBase> toolCalls;

  /** For tool result */
  @SerializedName("tool_call_id")
  String toolCallId;

  /** tool name */
  @SerializedName("name")
  String name;

  /** for multi modal message type: [text|image_url] image_url: url detail */
  private List<MessageContentBase> contents;

  /** chain of thought content */
  @SerializedName("reasoning_content")
  String reasoningContent;
}
