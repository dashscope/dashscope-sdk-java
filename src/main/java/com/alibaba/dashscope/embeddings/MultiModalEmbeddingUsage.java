package com.alibaba.dashscope.embeddings;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MultiModalEmbeddingUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("image_tokens")
  private Integer imageTokens;

  @SerializedName("image_count")
  private Integer imageCount;

  private Double duration;

  // forward compatible with multimodal-embedding-one-peace-v1
  @SerializedName("total_usage")
  private Integer totalUsage;

  private MultiModalEmbeddingsUsageInfo image;
  private MultiModalEmbeddingsUsageInfo audio;
  private MultiModalEmbeddingsUsageInfo text;
}
