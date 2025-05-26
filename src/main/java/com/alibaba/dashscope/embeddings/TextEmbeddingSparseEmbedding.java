package com.alibaba.dashscope.embeddings;

import lombok.Data;


@Data
public class TextEmbeddingSparseEmbedding {
  private Integer index;

  private Double value;

  private String token;
}
