// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.config.ConfigResolver;
import com.alibaba.dashscope.resources.chat.ChatAsyncService;
import com.alibaba.dashscope.resources.chat.ChatService;
import com.alibaba.dashscope.resources.embeddings.EmbeddingAsyncService;
import com.alibaba.dashscope.resources.embeddings.EmbeddingService;
import com.alibaba.dashscope.resources.rerank.RerankAsyncService;
import com.alibaba.dashscope.resources.rerank.RerankService;
import com.alibaba.dashscope.transport.HttpTransport;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;

/**
 * DashScope 3.0 asynchronous client (design §2.3): same resource tree as {@link DashScopeClient},
 * returning {@link java.util.concurrent.CompletableFuture}; streaming surfaces as
 * {@link java.util.concurrent.Flow.Publisher}. Work runs on a daemon executor owned by the client
 * ({@link #close()} shuts it down); derived clients share the parent's executor and pool.
 */
public final class DashScopeAsyncClient implements AutoCloseable {

  private final ClientOptions options;
  private final HttpTransport transport;
  private final ExecutorService executor;
  private final boolean ownsExecutor;
  private final ChatAsyncService chat;
  private final EmbeddingAsyncService embeddings;
  private final RerankAsyncService rerank;

  private DashScopeAsyncClient(
      ClientOptions options, HttpTransport transport, ExecutorService executor, boolean ownsExecutor) {
    this.options = options;
    this.transport = transport;
    this.executor = executor;
    this.ownsExecutor = ownsExecutor;
    this.chat = new ChatAsyncService(new ChatService(transport), executor);
    this.embeddings = new EmbeddingAsyncService(new EmbeddingService(transport), executor);
    this.rerank = new RerankAsyncService(new RerankService(transport), executor);
  }

  public static Builder builder() {
    return new Builder();
  }

  public ChatAsyncService chat() {
    return chat;
  }

  public EmbeddingAsyncService embeddings() {
    return embeddings;
  }

  public RerankAsyncService rerank() {
    return rerank;
  }

  /** Derive a client with extra overrides, sharing this client's pool and executor. */
  public DashScopeAsyncClient withOptions(ClientOptions overrides) {
    ClientOptions merged = DashScopeClient.merge(options, overrides);
    return new DashScopeAsyncClient(
        merged, transport.derive(ConfigResolver.resolve(merged)), executor, false);
  }

  @Override
  public void close() {
    transport.close();
    if (ownsExecutor) {
      executor.shutdown();
    }
  }

  private static ExecutorService newExecutor() {
    ThreadFactory factory =
        new ThreadFactory() {
          private final AtomicInteger seq = new AtomicInteger();

          @Override
          public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "dashscope-async-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
          }
        };
    return Executors.newCachedThreadPool(factory);
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

    public DashScopeAsyncClient build() {
      ClientOptions built = options.build();
      return new DashScopeAsyncClient(
          built, new HttpTransport(ConfigResolver.resolve(built)), newExecutor(), true);
    }
  }
}
