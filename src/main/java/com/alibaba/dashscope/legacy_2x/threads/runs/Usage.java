// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.threads.runs;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Deprecated
public final class Usage {
  @SerializedName("prompt_tokens")
  private Integer promptTokens;

  @SerializedName("completion_tokens")
  private Integer completionTokens;

  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("output_tokens")
  private Integer outputTokens;

  @SerializedName("total_tokens")
  private Integer totalTokens;
}
