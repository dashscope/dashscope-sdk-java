// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.rerank;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.types.rerank.RerankCreateParams;
import com.alibaba.dashscope.v3.types.rerank.RerankResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link RerankService}. */
public final class RerankAsyncService {

  private final RerankService sync;
  private final Executor executor;

  public RerankAsyncService(RerankService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<RerankResponse> create(RerankCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<RerankResponse> create(
      RerankCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }
}
