// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.completions;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One non-streaming choice. */
public class Choice {
  @JsonProperty("index")
  private int index;

  @JsonProperty("finish_reason")
  private String finishReason;

  @JsonProperty("message")
  private ChatCompletionMessage message;

  public int getIndex() {
    return index;
  }

  public String getFinishReason() {
    return finishReason;
  }

  public ChatCompletionMessage getMessage() {
    return message;
  }
}
