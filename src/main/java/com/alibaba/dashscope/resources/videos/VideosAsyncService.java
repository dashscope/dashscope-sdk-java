// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.videos;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.videos.VideoGenerateParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link VideosService}. */
public final class VideosAsyncService {
  private final VideosService sync;
  private final Executor executor;
  private final VideoJobsAsyncService jobs;

  public VideosAsyncService(VideosService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
    this.jobs = new VideoJobsAsyncService(sync.jobs(), executor);
  }

  public VideoJobsAsyncService jobs() {
    return jobs;
  }

  public CompletableFuture<Job> generate(VideoGenerateParams params) {
    return generate(params, RequestOptions.NONE);
  }

  public CompletableFuture<Job> generate(VideoGenerateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.generate(params, options), executor);
  }
}
