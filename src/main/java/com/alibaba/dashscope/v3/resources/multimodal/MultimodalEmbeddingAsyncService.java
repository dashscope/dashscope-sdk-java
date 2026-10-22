// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.multimodal;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalEmbeddingCreateParams;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalEmbeddingResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link MultimodalEmbeddingService}. */
public final class MultimodalEmbeddingAsyncService {

  private final MultimodalEmbeddingService sync;
  private final Executor executor;

  public MultimodalEmbeddingAsyncService(MultimodalEmbeddingService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<MultimodalEmbeddingResponse> create(
      MultimodalEmbeddingCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<MultimodalEmbeddingResponse> create(
      MultimodalEmbeddingCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }
}
