// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.tingwu;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.AsyncFutures;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.tingwu.TingWuJobParams;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link TingWuJobsService}. */
public final class TingWuJobsAsyncService {
  private final TingWuJobsService sync;
  private final Executor executor;

  public TingWuJobsAsyncService(TingWuJobsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<Job> create(TingWuJobParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<Job> create(TingWuJobParams params, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.create(params, options), executor);
  }

  public CompletableFuture<Job> fetch(String taskId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.fetch(taskId, options), executor);
  }

  public CompletableFuture<Job> wait(String taskId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.wait(taskId, options), executor);
  }

  public CompletableFuture<Job> cancel(String taskId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.cancel(taskId, options), executor);
  }

  public CompletableFuture<Page<Job>> list(Map<String, String> query, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.list(query, options), executor);
  }
}
