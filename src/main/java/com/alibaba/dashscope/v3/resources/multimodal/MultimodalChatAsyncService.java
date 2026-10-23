// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.multimodal;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.RxStreams;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatCompletion;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatCreateParams;
import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

  public Flowable<MultimodalChatChunk> createStreaming(MultimodalChatCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public Flowable<MultimodalChatChunk> createStreaming(
      MultimodalChatCreateParams params, RequestOptions options) {
    return RxStreams.fromStream(() -> sync.createStreaming(params, options), executor);
  }
}
