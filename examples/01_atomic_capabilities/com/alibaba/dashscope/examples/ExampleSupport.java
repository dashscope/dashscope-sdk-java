// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.DashScopeClient;

/**
 * Shared entry helpers for all examples: the API key comes only from the config chain
 * ({@code -Ddashscope.api.key=} / {@code DASHSCOPE_API_KEY} / config file); when missing, print
 * guidance and exit non-zero (design §8 规范).
 */
public final class ExampleSupport {

  private ExampleSupport() {}

  public static String requireApiKey() {
    String key = System.getProperty("dashscope.api.key");
    if (key == null || key.isEmpty()) {
      key = System.getenv("DASHSCOPE_API_KEY");
    }
    if (key == null || key.isEmpty()) {
      System.err.println(
          "error: no API key. Set DASHSCOPE_API_KEY (or -Ddashscope.api.key=sk-...); "
              + "see https://www.alibabacloud.com/help/en/model-studio/get-api-key");
      System.exit(1);
    }
    return key;
  }

  /** Sync client from the config chain (env/sysprops/config file); close it with try-with-resources. */
  public static DashScopeClient client() {
    requireApiKey();
    return DashScopeClient.builder().build();
  }

  public static DashScopeAsyncClient asyncClient() {
    requireApiKey();
    return DashScopeAsyncClient.builder().build();
  }
}
