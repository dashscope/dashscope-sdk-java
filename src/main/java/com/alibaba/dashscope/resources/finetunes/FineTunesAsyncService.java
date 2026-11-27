// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.finetunes;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.AsyncFutures;
import com.alibaba.dashscope.resources.RxStreams;
import com.alibaba.dashscope.types.common.JobStatus;
import com.alibaba.dashscope.types.finetunes.FineTune;
import com.alibaba.dashscope.types.finetunes.FineTuneCreateParams;
import com.alibaba.dashscope.types.finetunes.FineTuneEvent;
import com.alibaba.dashscope.types.finetunes.FineTuneList;
import com.alibaba.dashscope.types.finetunes.JobLogs;
import io.reactivex.Flowable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link FineTunesService}; {@code streamEvents} surfaces as a Publisher. */
public final class FineTunesAsyncService {
  private final FineTunesService sync;
  private final Executor executor;

  public FineTunesAsyncService(FineTunesService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<FineTune> create(FineTuneCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<FineTune> create(FineTuneCreateParams params, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.create(params, options), executor);
  }

  public CompletableFuture<FineTune> get(String jobId) {
    return get(jobId, RequestOptions.NONE);
  }

  public CompletableFuture<FineTune> get(String jobId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.get(jobId, options), executor);
  }

  public CompletableFuture<FineTuneList> list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public CompletableFuture<FineTuneList> list(int pageNo, int pageSize, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.list(pageNo, pageSize, options), executor);
  }

  public CompletableFuture<JobStatus> cancel(String jobId) {
    return cancel(jobId, RequestOptions.NONE);
  }

  public CompletableFuture<JobStatus> cancel(String jobId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.cancel(jobId, options), executor);
  }

  public CompletableFuture<JobStatus> delete(String jobId) {
    return delete(jobId, RequestOptions.NONE);
  }

  public CompletableFuture<JobStatus> delete(String jobId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.delete(jobId, options), executor);
  }

  public CompletableFuture<JobLogs> logs(String jobId, int offset, int lines) {
    return logs(jobId, offset, lines, RequestOptions.NONE);
  }

  public CompletableFuture<JobLogs> logs(
      String jobId, int offset, int lines, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.logs(jobId, offset, lines, options), executor);
  }

  public Flowable<FineTuneEvent> streamEvents(String jobId) {
    return streamEvents(jobId, RequestOptions.NONE);
  }

  public Flowable<FineTuneEvent> streamEvents(String jobId, RequestOptions options) {
    return RxStreams.fromIterable(() -> sync.streamEvents(jobId, options), executor);
  }

  public CompletableFuture<FineTune> wait(String jobId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.wait(jobId, options), executor);
  }

  public CompletableFuture<FineTune> wait(
      String jobId, Duration waitTimeout, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.wait(jobId, waitTimeout, options), executor);
  }
}
