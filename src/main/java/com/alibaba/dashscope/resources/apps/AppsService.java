// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.apps;

import com.alibaba.dashscope.transport.HttpTransport;

/** {@code apps} resource group: Bailian application calls. */
public final class AppsService {
  private final AppCompletionsService completions;

  public AppsService(HttpTransport transport) {
    this.completions = new AppCompletionsService(transport);
  }

  public AppCompletionsService completions() {
    return completions;
  }
}
