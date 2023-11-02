// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class MultiModalEmbeddingItemAudio extends MultiModalEmbeddingItemBase {
  public String audio;

  public MultiModalEmbeddingItemAudio(String audio, Double factor) {
    super(factor);
    this.audio = audio;
  }

  public MultiModalEmbeddingItemAudio(String audio) {
    this.audio = audio;
  }

  @Override
  public String getModal() {
    return "audio";
  }

  @Override
  public String getContent() {
    return audio;
  }

  @Override
  public void setContent(String content) {
    this.audio = content;
  }
}
