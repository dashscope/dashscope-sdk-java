package com.alibaba.dashscope.tokenizers;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TokenizationUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;
}
