// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class MultiModalEmbeddingItemBase {
  protected Double factor;

  public MultiModalEmbeddingItemBase(Double factor) {
    this.factor = factor;
  }

  public MultiModalEmbeddingItemBase() {
    factor = null;
  }
}
