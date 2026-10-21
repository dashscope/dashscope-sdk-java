// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.config.ConfigResolver;
import com.alibaba.dashscope.config.ResolvedConfig;
import com.alibaba.dashscope.resources.chat.ChatService;
import com.alibaba.dashscope.resources.embeddings.EmbeddingService;
import com.alibaba.dashscope.resources.rerank.RerankService;
import com.alibaba.dashscope.transport.HttpTransport;
import java.time.Duration;
import okhttp3.OkHttpClient;

/**
 * DashScope 3.0 synchronous client (design §2). Entry point:
 *
 * <pre>{@code
 * try (DashScopeClient client = DashScopeClient.builder().apiKey("...").build()) {
 *   ChatCompletion r = client.chat().completions().create(params);
 * }
 * }</pre>
 *
 * <p>{@link #withOptions(ClientOptions)} derives a client that shares the underlying connection
 * pool; a derived client must not outlive its parent.
 */
public final class DashScopeClient implements AutoCloseable {

  private final ClientOptions options;
  private final ResolvedConfig config;
  private final HttpTransport transport;
  private final ChatService chat;
  private final EmbeddingService embeddings;
  private final RerankService rerank;

  private DashScopeClient(ClientOptions options, HttpTransport transport) {
    this.options = options;
    this.config = transport.config();
    this.transport = transport;
    this.chat = new ChatService(transport);
    this.embeddings = new EmbeddingService(transport);
    this.rerank = new RerankService(transport);
  }

  public static Builder builder() {
    return new Builder();
  }

  public ChatService chat() {
    return chat;
  }

  public EmbeddingService embeddings() {
    return embeddings;
  }

  public RerankService rerank() {
    return rerank;
  }

  /** The resolved configuration (config chain applied). */
  public ResolvedConfig config() {
    return config;
  }

  /** Derive a client with extra overrides, sharing this client's connection pool. */
  public DashScopeClient withOptions(ClientOptions overrides) {
    ClientOptions merged = merge(options, overrides);
    return new DashScopeClient(merged, transport.derive(ConfigResolver.resolve(merged)));
  }

  @Override
  public void close() {
    transport.close();
  }

  static ClientOptions merge(ClientOptions base, ClientOptions overrides) {
    ClientOptions.Builder b = base.toBuilder();
    if (overrides.getApiKey() != null) {
      b.apiKey(overrides.getApiKey());
    }
    if (overrides.getBaseUrl() != null) {
      b.baseUrl(overrides.getBaseUrl());
    }
    if (overrides.getWsBaseUrl() != null) {
      b.wsBaseUrl(overrides.getWsBaseUrl());
    }
    if (overrides.getWorkspace() != null) {
      b.workspace(overrides.getWorkspace());
    }
    if (overrides.getRegion() != null) {
      b.region(overrides.getRegion());
    }
    if (overrides.getTimeout() != null) {
      b.timeout(overrides.getTimeout());
    }
    if (overrides.getMaxRetries() != null) {
      b.maxRetries(overrides.getMaxRetries());
    }
    if (overrides.getRaiseOnError() != null) {
      b.raiseOnError(overrides.getRaiseOnError());
    }
    if (overrides.getHttpClient() != null) {
      b.httpClient(overrides.getHttpClient());
    }
    return b.build();
  }

  public static final class Builder {
    private final ClientOptions.Builder options = ClientOptions.builder();

    private Builder() {}

    public Builder apiKey(String apiKey) {
      options.apiKey(apiKey);
      return this;
    }

    public Builder baseUrl(String baseUrl) {
      options.baseUrl(baseUrl);
      return this;
    }

    public Builder wsBaseUrl(String wsBaseUrl) {
      options.wsBaseUrl(wsBaseUrl);
      return this;
    }

    public Builder workspace(String workspace) {
      options.workspace(workspace);
      return this;
    }

    public Builder region(String region) {
      options.region(region);
      return this;
    }

    public Builder timeout(Duration timeout) {
      options.timeout(timeout);
      return this;
    }

    public Builder maxRetries(Integer maxRetries) {
      options.maxRetries(maxRetries);
      return this;
    }

    public Builder raiseOnError(Boolean raiseOnError) {
      options.raiseOnError(raiseOnError);
      return this;
    }

    public Builder httpClient(OkHttpClient httpClient) {
      options.httpClient(httpClient);
      return this;
    }

    public DashScopeClient build() {
      ClientOptions built = options.build();
      return new DashScopeClient(built, new HttpTransport(ConfigResolver.resolve(built)));
    }
  }
}
