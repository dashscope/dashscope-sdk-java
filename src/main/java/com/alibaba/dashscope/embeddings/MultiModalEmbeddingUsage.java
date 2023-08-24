package com.alibaba.dashscope.embeddings;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

// "usage":{"image":{"measure":1,"weight":1},
// "total_usage":4,
// "audio":{"measure":1,"weight":2},
// "text":{"measure":1,"weight":1}}
@Data
public class MultiModalEmbeddingUsage {
  @SerializedName("total_usage")
  private Integer totalUsage;

  private MultiModalEmbeddingsUsageInfo image;
  private MultiModalEmbeddingsUsageInfo audio;
  private MultiModalEmbeddingsUsageInfo text;
}
