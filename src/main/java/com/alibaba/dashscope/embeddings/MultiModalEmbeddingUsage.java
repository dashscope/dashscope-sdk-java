package com.alibaba.dashscope.embeddings;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class MultiModalEmbeddingUsage {
  @SerializedName("input_tokens")
  private Integer inputTokens;

  @SerializedName("image_count")
  private Integer imageCount;

  private Double duration;
}
