// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.decision;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.types.decision.DecisionCreateParams;
import com.alibaba.dashscope.types.decision.DecisionResult;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link DecisionService} (no streaming on this endpoint). */
public final class DecisionAsyncService {
  private final DecisionService sync;
  private final Executor executor;

  public DecisionAsyncService(DecisionService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<DecisionResult> create(DecisionCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<DecisionResult> create(
      DecisionCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }
}
