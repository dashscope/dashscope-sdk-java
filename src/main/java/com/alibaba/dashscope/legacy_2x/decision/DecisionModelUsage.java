// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.decision;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** The token usage of the decision model api. */
@Data
@Deprecated
public class DecisionModelUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;
}
