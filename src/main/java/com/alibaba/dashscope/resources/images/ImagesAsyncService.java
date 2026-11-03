// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.images;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.AsyncFutures;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.images.ImageGenerateParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link ImagesService}. */
public final class ImagesAsyncService {
  private final ImagesService sync;
  private final Executor executor;
  private final ImageJobsAsyncService jobs;

  public ImagesAsyncService(ImagesService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
    this.jobs = new ImageJobsAsyncService(sync.jobs(), executor);
  }

  public ImageJobsAsyncService jobs() {
    return jobs;
  }

  public CompletableFuture<Job> generate(ImageGenerateParams params) {
    return generate(params, RequestOptions.NONE);
  }

  public CompletableFuture<Job> generate(ImageGenerateParams params, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.generate(params, options), executor);
  }
}
