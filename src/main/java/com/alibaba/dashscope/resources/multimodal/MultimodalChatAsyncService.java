// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.multimodal;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.FlowPublishers;
import com.alibaba.dashscope.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCompletion;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCreateParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/** Async variant of {@link MultimodalChatService}. */
public final class MultimodalChatAsyncService {

  private final MultimodalChatService sync;
  private final Executor executor;

  public MultimodalChatAsyncService(MultimodalChatService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<MultimodalChatCompletion> create(MultimodalChatCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<MultimodalChatCompletion> create(
      MultimodalChatCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }

  public Flow.Publisher<MultimodalChatChunk> createStreaming(MultimodalChatCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public Flow.Publisher<MultimodalChatChunk> createStreaming(
      MultimodalChatCreateParams params, RequestOptions options) {
    return FlowPublishers.fromStream(() -> sync.createStreaming(params, options), executor);
  }
}
