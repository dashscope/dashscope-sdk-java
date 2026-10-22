// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.apps;

import java.util.concurrent.Executor;

/** Async {@code apps} resource group. */
public final class AppsAsyncService {
  private final AppCompletionsAsyncService completions;

  public AppsAsyncService(AppsService sync, Executor executor) {
    this.completions = new AppCompletionsAsyncService(sync.completions(), executor);
  }

  public AppCompletionsAsyncService completions() {
    return completions;
  }
}
