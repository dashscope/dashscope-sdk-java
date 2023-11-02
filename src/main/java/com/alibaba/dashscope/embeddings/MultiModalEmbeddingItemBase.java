// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemBase;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public abstract class MultiModalEmbeddingItemBase implements MultiModalMessageItemBase {
  protected Double factor;

  public MultiModalEmbeddingItemBase(Double factor) {
    this.factor = factor;
  }

  public MultiModalEmbeddingItemBase() {
    factor = null;
  }
}
