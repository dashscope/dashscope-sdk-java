// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.apps;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.FlowPublishers;
import com.alibaba.dashscope.v3.types.apps.AppCompletion;
import com.alibaba.dashscope.v3.types.apps.AppCompletionCreateParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/** Async variant of {@link AppCompletionsService}. */
public final class AppCompletionsAsyncService {
  private final AppCompletionsService sync;
  private final Executor executor;

  public AppCompletionsAsyncService(AppCompletionsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<AppCompletion> create(AppCompletionCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<AppCompletion> create(
      AppCompletionCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }

  public Flow.Publisher<AppCompletion> createStreaming(AppCompletionCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public Flow.Publisher<AppCompletion> createStreaming(
      AppCompletionCreateParams params, RequestOptions options) {
    return FlowPublishers.fromStream(() -> sync.createStreaming(params, options), executor);
  }
}
