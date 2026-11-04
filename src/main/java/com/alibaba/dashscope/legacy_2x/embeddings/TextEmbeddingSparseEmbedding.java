package com.alibaba.dashscope.legacy_2x.embeddings;

import lombok.Data;

@Data
@Deprecated
public class TextEmbeddingSparseEmbedding {
  private Integer index;

  private Double value;

  private String token;
}
