// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.embeddings;

import com.alibaba.dashscope.legacy_2x.aigc.multimodalconversation.MultiModalMessageItemBase;
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
