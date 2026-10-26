// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.embeddings;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.embeddings.BatchEmbeddingCreateParams;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link EmbeddingJobsService}. */
public final class EmbeddingJobsAsyncService {
  private final EmbeddingJobsService sync;
  private final Executor executor;

  public EmbeddingJobsAsyncService(EmbeddingJobsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<Job> create(BatchEmbeddingCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<Job> create(BatchEmbeddingCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }

  public CompletableFuture<Job> fetch(String taskId, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.fetch(taskId, options), executor);
  }

  public CompletableFuture<Job> wait(String taskId, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.wait(taskId, options), executor);
  }

  public CompletableFuture<Job> cancel(String taskId, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.cancel(taskId, options), executor);
  }

  public CompletableFuture<Page<Job>> list(Map<String, String> query, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.list(query, options), executor);
  }
}
