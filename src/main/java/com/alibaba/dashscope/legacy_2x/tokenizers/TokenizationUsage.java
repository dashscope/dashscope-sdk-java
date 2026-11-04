package com.alibaba.dashscope.legacy_2x.tokenizers;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
@Deprecated
public class TokenizationUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;
}
