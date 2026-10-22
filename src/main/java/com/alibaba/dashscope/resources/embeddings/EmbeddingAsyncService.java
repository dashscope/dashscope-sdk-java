// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.embeddings;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.types.embeddings.EmbeddingResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link EmbeddingService}. */
public final class EmbeddingAsyncService {

  private final EmbeddingService sync;
  private final Executor executor;
  private final EmbeddingJobsAsyncService jobs;

  public EmbeddingAsyncService(EmbeddingService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
    this.jobs = new EmbeddingJobsAsyncService(sync.jobs(), executor);
  }

  /** The async batch-task handle: create/fetch/wait/cancel/list. */
  public EmbeddingJobsAsyncService jobs() {
    return jobs;
  }

  public CompletableFuture<EmbeddingResponse> create(EmbeddingCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<EmbeddingResponse> create(
      EmbeddingCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }
}
