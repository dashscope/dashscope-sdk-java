// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

/** OkHttp request tags carrying per-call state into application interceptors. */
public final class RequestTags {
  private RequestTags() {}

  /** Marker: SSE / WebSocket-upgrade requests must not be auto-retried (design §6). */
  public static final class NoRetry {
    public static final NoRetry INSTANCE = new NoRetry();

    private NoRetry() {}
  }

  /** Per-request maxRetries override. */
  public static final class MaxRetries {
    public final int value;

    public MaxRetries(int value) {
      this.value = value;
    }
  }

  /** Functional-area tag for the x-dashscope-sdk-client header (e.g. "aigc", "embeddings"). */
  public static final class Module {
    public final String value;

    public Module(String value) {
      this.value = value;
    }
  }

  /** Per-request auth material resolved from config + RequestOptions overrides. */
  public static final class Auth {
    public final String apiKey;
    public final String workspace;

    public Auth(String apiKey, String workspace) {
      this.apiKey = apiKey;
      this.workspace = workspace;
    }
  }
}
