// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.jobs;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.AsyncFutures;
import com.alibaba.dashscope.types.common.Page;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Async variant of {@link JobsService}. */
public final class JobsAsyncService {
  private final JobsService sync;
  private final Executor executor;

  public JobsAsyncService(JobsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<Job> create(String servicePath, JsonNode body, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.create(servicePath, body, options), executor);
  }

  public CompletableFuture<Job> create(
      String servicePath, JsonNode body, Map<String, String> extraHeaders, RequestOptions options) {
    return AsyncFutures.supplyAsync(
        () -> sync.create(servicePath, body, extraHeaders, options), executor);
  }

  public CompletableFuture<Job> fetch(String taskId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.fetch(taskId, options), executor);
  }

  public CompletableFuture<Job> wait(String taskId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.wait(taskId, options), executor);
  }

  public CompletableFuture<Job> wait(String taskId, Duration waitTimeout, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.wait(taskId, waitTimeout, options), executor);
  }

  public CompletableFuture<Job> cancel(String taskId, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.cancel(taskId, options), executor);
  }

  public CompletableFuture<Page<Job>> list(Map<String, String> query, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.list(query, options), executor);
  }
}
