// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.threads.runs;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class Usage {
  @SerializedName("prompt_tokens")
  private Integer promptTokens;

  @SerializedName("completion_tokens")
  private Integer completionTokens;

  @SerializedName("total_tokens")
  private Integer totalTokens;
}
