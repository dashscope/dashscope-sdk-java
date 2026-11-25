// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.AsyncFutures;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobsAsyncService;
import com.alibaba.dashscope.types.audio.AsrTranscribeParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Async variant of {@link AsrService}: batch transcription only. Realtime {@code recognize} is
 * sync-only by design (design §2: ws duplex surfaces live on the sync client).
 */
public final class AsrAsyncService {
  private final AsrService sync;
  private final JobsAsyncService jobs;
  private final Executor executor;

  public AsrAsyncService(AsrService sync, Executor executor) {
    this.sync = sync;
    this.jobs = new JobsAsyncService(sync.jobs(), executor);
    this.executor = executor;
  }

  public CompletableFuture<Job> transcribe(AsrTranscribeParams params) {
    return transcribe(params, RequestOptions.NONE);
  }

  public CompletableFuture<Job> transcribe(AsrTranscribeParams params, RequestOptions options) {
    return AsyncFutures.supplyAsync(() -> sync.transcribe(params, options), executor);
  }

  /** The async-job handle for transcription: create/fetch/wait/cancel/list. */
  public JobsAsyncService jobs() {
    return jobs;
  }
}
