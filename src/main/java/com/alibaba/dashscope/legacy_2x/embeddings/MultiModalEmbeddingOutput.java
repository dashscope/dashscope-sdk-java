package com.alibaba.dashscope.legacy_2x.embeddings;

import java.util.List;
import lombok.Data;

@Data
public class MultiModalEmbeddingOutput {
  private List<MultiModalEmbeddingResultItem> embeddings;

  // forward compatible with multimodal-embedding-one-peace-v1
  private List<Double> embedding;
}
