// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** The token usage of the decision model api. */
@Data
public class DecisionModelUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;
}
