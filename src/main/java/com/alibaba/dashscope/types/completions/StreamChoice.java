// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.completions;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One streaming choice: carries a {@link Delta} instead of a full message. */
public class StreamChoice {
  @JsonProperty("index")
  private int index;

  @JsonProperty("finish_reason")
  private String finishReason;

  @JsonProperty("delta")
  private Delta delta;

  public int getIndex() {
    return index;
  }

  public String getFinishReason() {
    return finishReason;
  }

  public Delta getDelta() {
    return delta;
  }
}
