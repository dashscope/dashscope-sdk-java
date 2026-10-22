// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.multimodal;

import java.util.concurrent.Executor;

/** Async variant of {@link MultimodalService}. */
public final class MultimodalAsyncService {
  private final MultimodalChatAsyncService chat;
  private final MultimodalEmbeddingAsyncService embeddings;

  public MultimodalAsyncService(MultimodalService sync, Executor executor) {
    this.chat = new MultimodalChatAsyncService(sync.chat(), executor);
    this.embeddings = new MultimodalEmbeddingAsyncService(sync.embeddings(), executor);
  }

  public MultimodalChatAsyncService chat() {
    return chat;
  }

  public MultimodalEmbeddingAsyncService embeddings() {
    return embeddings;
  }
}
