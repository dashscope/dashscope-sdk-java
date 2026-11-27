// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import java.time.Duration;
import java.util.concurrent.Executor;
import okhttp3.OkHttpClient;

/**
 * Immutable client options (design §2). Every field is an <em>explicit</em> value: null means "fall
 * through the config chain" (builder &gt; JVM system property &gt; environment variable &gt; {@code
 * ~/.dashscope/config.toml} &gt; default, see {@code config/ConfigResolver}).
 */
public final class ClientOptions {
  private final String apiKey;
  private final String baseUrl;
  private final String wsBaseUrl;
  private final String workspace;
  private final String region;
  private final Duration timeout;
  private final Integer maxRetries;
  private final Boolean raiseOnError;
  private final Duration wsPingInterval;
  private final OkHttpClient httpClient;
  private final Executor executor;

  private ClientOptions(Builder b) {
    this.apiKey = b.apiKey;
    this.baseUrl = b.baseUrl;
    this.wsBaseUrl = b.wsBaseUrl;
    this.workspace = b.workspace;
    this.region = b.region;
    this.timeout = b.timeout;
    this.maxRetries = b.maxRetries;
    this.raiseOnError = b.raiseOnError;
    this.wsPingInterval = b.wsPingInterval;
    this.httpClient = b.httpClient;
    this.executor = b.executor;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    Builder b = new Builder();
    b.apiKey = apiKey;
    b.baseUrl = baseUrl;
    b.wsBaseUrl = wsBaseUrl;
    b.workspace = workspace;
    b.region = region;
    b.timeout = timeout;
    b.maxRetries = maxRetries;
    b.raiseOnError = raiseOnError;
    b.wsPingInterval = wsPingInterval;
    b.httpClient = httpClient;
    b.executor = executor;
    return b;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public String getWsBaseUrl() {
    return wsBaseUrl;
  }

  public String getWorkspace() {
    return workspace;
  }

  public String getRegion() {
    return region;
  }

  /** Single-request timeout (design §3). */
  public Duration getTimeout() {
    return timeout;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  /** Default true: failures raise exceptions; false: 2.x-style {@code isError()} returns. */
  public Boolean getRaiseOnError() {
    return raiseOnError;
  }

  /**
   * WebSocket ping interval (audio design §12#1). Default 0 = off (aligned with Python); behind
   * NATs or proxies that silently drop idle connections, 20–30 s is recommended so half-open
   * connections are detected instead of hanging forever.
   */
  public Duration getWsPingInterval() {
    return wsPingInterval;
  }

  /** Externally injected OkHttpClient; the caller then owns its lifecycle. */
  public OkHttpClient getHttpClient() {
    return httpClient;
  }

  /** Externally injected async executor; the caller owns its lifecycle. */
  public Executor getExecutor() {
    return executor;
  }

  public static final class Builder {
    private String apiKey;
    private String baseUrl;
    private String wsBaseUrl;
    private String workspace;
    private String region;
    private Duration timeout;
    private Integer maxRetries;
    private Boolean raiseOnError;
    private Duration wsPingInterval;
    private OkHttpClient httpClient;
    private Executor executor;

    private Builder() {}

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder wsBaseUrl(String wsBaseUrl) {
      this.wsBaseUrl = wsBaseUrl;
      return this;
    }

    public Builder workspace(String workspace) {
      this.workspace = workspace;
      return this;
    }

    /** One of the six MaaS gateway regions; default {@code cn-beijing}. */
    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder raiseOnError(Boolean raiseOnError) {
      this.raiseOnError = raiseOnError;
      return this;
    }

    /** See {@link #getWsPingInterval()}; {@code Duration.ZERO} disables (the default). */
    public Builder wsPingInterval(Duration wsPingInterval) {
      this.wsPingInterval = wsPingInterval;
      return this;
    }

    public Builder httpClient(OkHttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    public Builder executor(Executor executor) {
      this.executor = executor;
      return this;
    }

    public ClientOptions build() {
      return new ClientOptions(this);
    }
  }
}
