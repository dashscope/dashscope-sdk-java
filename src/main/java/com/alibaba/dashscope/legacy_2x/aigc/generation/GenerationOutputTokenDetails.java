package com.alibaba.dashscope.legacy_2x.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Deprecated
public class GenerationOutputTokenDetails {
  @SerializedName("reasoning_tokens")
  private Integer reasoningTokens;
}
