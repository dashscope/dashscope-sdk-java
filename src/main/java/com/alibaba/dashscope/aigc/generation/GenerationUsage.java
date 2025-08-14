// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class GenerationUsage {

  @Data
  @SuperBuilder
  public static class PromptTokensDetails {

    @Data
    public static class CacheCreation {
      @SerializedName("ephemeral_5m_input_tokens")
      private Integer ephemeral5mInputTokens;

      @SerializedName("ephemeral_1h_input_tokens")
      private Integer ephemeral1hInputTokens;
    }

    @SerializedName("cache_type")
    private String cacheType;

    @SerializedName("cached_tokens")
    private Integer cachedTokens;

    @SerializedName("cache_creation_input_tokens")
    private Integer cacheCreationInputTokens;

    @SerializedName("cache_creation")
    private CacheCreation cacheCreation;
  }

  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("output_tokens")
  private Integer outputTokens;

  @SerializedName("total_tokens")
  private Integer totalTokens;

  @SerializedName("output_tokens_details")
  private GenerationOutputTokenDetails outputTokensDetails;

  @SerializedName("prompt_tokens_details")
  private PromptTokensDetails promptTokensDetails;
}
