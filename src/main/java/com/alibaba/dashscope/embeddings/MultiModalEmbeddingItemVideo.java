// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class MultiModalEmbeddingItemVideo extends MultiModalEmbeddingItemBase {
  public String video;

  public MultiModalEmbeddingItemVideo(String video, Double factor) {
    super(factor);
    this.video = video;
  }

  public MultiModalEmbeddingItemVideo(String video) {
    this.video = video;
  }

  @Override
  public String getModal() {
    return "video";
  }

  @Override
  public String getContent() {
    return video;
  }

  @Override
  public void setContent(String content) {
    this.video = content;
  }
}
