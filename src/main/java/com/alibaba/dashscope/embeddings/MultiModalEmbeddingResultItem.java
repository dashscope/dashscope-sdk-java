package com.alibaba.dashscope.embeddings;

import java.util.List;
import lombok.Data;

@Data
public class MultiModalEmbeddingResultItem {
  private Integer index;

  private List<Double> embedding;
}
