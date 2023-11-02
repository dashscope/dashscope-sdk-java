// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class MultiModalEmbeddingItemText extends MultiModalEmbeddingItemBase {
  public String text;

  public MultiModalEmbeddingItemText(String text, Double factor) {
    super(factor);
    this.text = text;
  }

  public MultiModalEmbeddingItemText(String text) {
    this.text = text;
  }

  @Override
  public String getModal() {
    return "text";
  }

  @Override
  public String getContent() {
    return text;
  }

  @Override
  public void setContent(String content) {
    this.text = content;
  }
}
