// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.images;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.types.common.Page;
import com.alibaba.dashscope.v3.types.images.ImageGenerateParams;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link ImageJobsService}. */
public final class ImageJobsAsyncService {
  private final ImageJobsService sync;
  private final Executor executor;

  ImageJobsAsyncService(ImageJobsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<Job> create(ImageGenerateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<Job> create(ImageGenerateParams params, RequestOptions options) {
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
