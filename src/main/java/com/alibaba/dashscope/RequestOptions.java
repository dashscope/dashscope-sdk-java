// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-call overrides (design §2): every resource method has a {@code (params, RequestOptions)}
 * overload. Null fields inherit the client-level resolved config.
 */
public final class RequestOptions {
  public static final RequestOptions NONE = builder().build();

  private final Duration timeout;
  private final Integer maxRetries;
  private final String apiKey;
  private final String workspace;
  private final Map<String, String> headers;

  private RequestOptions(Builder b) {
    this.timeout = b.timeout;
    this.maxRetries = b.maxRetries;
    this.apiKey = b.apiKey;
    this.workspace = b.workspace;
    this.headers =
        b.headers == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(b.headers));
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Single-request timeout; overrides the client default for this call only. */
  public Duration getTimeout() {
    return timeout;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getWorkspace() {
    return workspace;
  }

  /** Extra headers; applied last, may override SDK-set headers. */
  public Map<String, String> getHeaders() {
    return headers;
  }

  public static final class Builder {
    private Duration timeout;
    private Integer maxRetries;
    private String apiKey;
    private String workspace;
    private Map<String, String> headers;

    private Builder() {}

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder maxRetries(Integer maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder workspace(String workspace) {
      this.workspace = workspace;
      return this;
    }

    public Builder header(String name, String value) {
      if (this.headers == null) {
        this.headers = new LinkedHashMap<>();
      }
      this.headers.put(name, value);
      return this;
    }

    public Builder headers(Map<String, String> headers) {
      if (headers != null) {
        headers.forEach(this::header);
      }
      return this;
    }

    public RequestOptions build() {
      return new RequestOptions(this);
    }
  }
}
