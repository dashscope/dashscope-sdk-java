// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class GenerationUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("output_tokens")
  private Integer outputTokens;

  @SerializedName("total_tokens")
  private Integer totalTokens;

  @SerializedName("output_tokens_details")
  private GenerationOutputTokenDetails outputTokensDetails;
}
