// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class MultiModalEmbeddingItemImage extends MultiModalEmbeddingItemBase {
  public String image;

  public MultiModalEmbeddingItemImage(String image, Double factor) {
    super(factor);
    this.image = image;
  }

  public MultiModalEmbeddingItemImage(String image) {
    this.image = image;
  }
}
