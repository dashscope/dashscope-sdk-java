// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.tts;

import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class SpeechSynthesisUsage {
  private Integer characters;

  @SerializedName(ApiKeywords.INPUT_TOKENS)
  private Integer inputTokens;

  @SerializedName(ApiKeywords.OUTPUT_TOKENS)
  private Integer outputTokens;

  @SerializedName(ApiKeywords.TOTAL_TOKENS)
  private Integer totalTokens;
}
