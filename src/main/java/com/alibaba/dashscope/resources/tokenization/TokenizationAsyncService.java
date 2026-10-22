// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.tokenization;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.types.tokenization.TokenCount;
import com.alibaba.dashscope.types.tokenization.TokenizationCountParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link TokenizationService}. */
public final class TokenizationAsyncService {

  private final TokenizationService sync;
  private final Executor executor;

  public TokenizationAsyncService(TokenizationService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<TokenCount> count(TokenizationCountParams params) {
    return count(params, RequestOptions.NONE);
  }

  public CompletableFuture<TokenCount> count(
      TokenizationCountParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.count(params, options), executor);
  }
}
