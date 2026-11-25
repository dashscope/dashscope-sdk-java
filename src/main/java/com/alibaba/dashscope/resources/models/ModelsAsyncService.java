// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.models;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.AsyncFutures;
import com.alibaba.dashscope.types.models.Model;
import com.alibaba.dashscope.types.models.ModelList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link ModelsService}. */
public final class ModelsAsyncService {
  private final ModelsService sync;
  private final Executor executor;

  public ModelsAsyncService(ModelsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<ModelList> list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, null, RequestOptions.NONE);
  }

  public CompletableFuture<ModelList> list(
      int pageNo, int pageSize, String provider, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.list(pageNo, pageSize, provider, options), executor);
  }

  public CompletableFuture<Model> get(String model) {
    return get(model, RequestOptions.NONE);
  }

  public CompletableFuture<Model> get(String model, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.get(model, options), executor);
  }
}
