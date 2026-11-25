// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.multimodal;

import com.alibaba.dashscope.transport.HttpTransport;

/** {@code multimodal} resource group: {@code chat()} / {@code embeddings()}. */
public final class MultimodalService {
  private final MultimodalChatService chat;
  private final MultimodalEmbeddingService embeddings;

  public MultimodalService(HttpTransport transport) {
    this.chat = new MultimodalChatService(transport);
    this.embeddings = new MultimodalEmbeddingService(transport);
  }

  public MultimodalChatService chat() {
    return chat;
  }

  public MultimodalEmbeddingService embeddings() {
    return embeddings;
  }
}
