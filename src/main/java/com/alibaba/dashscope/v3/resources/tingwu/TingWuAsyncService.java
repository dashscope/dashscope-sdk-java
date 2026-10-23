// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.tingwu;

import java.util.concurrent.Executor;

/**
 * Async {@code tingwu}: the offline-task handle only — realtime sessions stay sync-only (Python v2
 * parity).
 */
public final class TingWuAsyncService {
  private final TingWuJobsAsyncService jobs;

  public TingWuAsyncService(TingWuService sync, Executor executor) {
    this.jobs = new TingWuJobsAsyncService(sync.jobs(), executor);
  }

  /** The offline-task handle: create/fetch/wait/cancel/list. */
  public TingWuJobsAsyncService jobs() {
    return jobs;
  }
}
