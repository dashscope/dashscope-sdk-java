// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.multimodal;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One multimodal choice (used by both the completion and, after merge, stream frames). */
public class MultimodalChoice {
  @JsonProperty("index")
  private int index;

  @JsonProperty("finish_reason")
  private String finishReason;

  @JsonProperty("message")
  private MultimodalOutputMessage message;

  public int getIndex() {
    return index;
  }

  public String getFinishReason() {
    return finishReason;
  }

  public MultimodalOutputMessage getMessage() {
    return message;
  }
}
