// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.rerank;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TextReRankUsage {
  @SerializedName("total_tokens")
  private Integer totalTokens;
}
