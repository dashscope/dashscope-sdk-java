// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.config;

import java.time.Duration;
import okhttp3.OkHttpClient;

/** Fully-resolved client configuration, produced once per client by {@link ConfigResolver}. */
public final class ResolvedConfig {
  private final String apiKey;
  private final String baseUrl;
  private final String wsBaseUrl;
  private final String workspace;
  private final String region;
  private final Duration timeout;
  private final int maxRetries;
  private final boolean raiseOnError;
  private final OkHttpClient httpClient;
  private final int maxRequests;
  private final int maxRequestsPerHost;
  private final Duration keepAlive;
  private final Duration wsPingInterval;

  ResolvedConfig(
      String apiKey,
      String baseUrl,
      String wsBaseUrl,
      String workspace,
      String region,
      Duration timeout,
      int maxRetries,
      boolean raiseOnError,
      OkHttpClient httpClient,
      int maxRequests,
      int maxRequestsPerHost,
      Duration keepAlive,
      Duration wsPingInterval) {
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.wsBaseUrl = wsBaseUrl;
    this.workspace = workspace;
    this.region = region;
    this.timeout = timeout;
    this.maxRetries = maxRetries;
    this.raiseOnError = raiseOnError;
    this.httpClient = httpClient;
    this.maxRequests = maxRequests;
    this.maxRequestsPerHost = maxRequestsPerHost;
    this.keepAlive = keepAlive;
    this.wsPingInterval = wsPingInterval;
  }

  /** Resolved API key; may be null (failure surfaces at request time as SdkException). */
  public String getApiKey() {
    return apiKey;
  }

  /** HTTP service root, e.g. {@code https://dashscope.aliyuncs.com/api/v1}. */
  public String getBaseUrl() {
    return baseUrl;
  }

  /** WebSocket service root, e.g. {@code wss://dashscope.aliyuncs.com/api-ws/v1/inference}. */
  public String getWsBaseUrl() {
    return wsBaseUrl;
  }

  public String getWorkspace() {
    return workspace;
  }

  public String getRegion() {
    return region;
  }

  /** Single-request timeout (design §3: one of the three timeout semantics). */
  public Duration getTimeout() {
    return timeout;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  /** true (default): failures raise DashScopeException; false: 2.x-style isError() returns. */
  public boolean isRaiseOnError() {
    return raiseOnError;
  }

  /** Externally injected OkHttpClient; null means the SDK builds and owns one. */
  public OkHttpClient getHttpClient() {
    return httpClient;
  }

  public int getMaxRequests() {
    return maxRequests;
  }

  public int getMaxRequestsPerHost() {
    return maxRequestsPerHost;
  }

  public Duration getKeepAlive() {
    return keepAlive;
  }

  /**
   * WebSocket ping interval (audio design §12#1). {@link Duration#ZERO} = off (aligned with
   * Python); behind NATs or proxies that silently drop idle connections, 20–30 s is recommended
   * so half-open connections are detected instead of hanging forever.
   */
  public Duration getWsPingInterval() {
    return wsPingInterval;
  }
}
