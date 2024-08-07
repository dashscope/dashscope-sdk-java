package com.alibaba.dashscope.aigc.completion;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class ChatCompletionUsage {
  @SerializedName("completion_tokens")
  private Integer completionTokens;

  @SerializedName("prompt_tokens")
  private Integer promptTokens;

  @SerializedName("total_tokens")
  private Integer totalTokens;
}
