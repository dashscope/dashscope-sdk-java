package com.alibaba.dashscope.embeddings;

import java.util.List;
import lombok.Data;

@Data
public class MultiModalEmbeddingOutput {
  private List<MultiModalEmbeddingResultItem> embeddings;
}
