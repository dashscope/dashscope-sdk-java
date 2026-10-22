// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.v3.config.ConfigResolver;
import com.alibaba.dashscope.v3.config.ResolvedConfig;
import com.alibaba.dashscope.v3.resources.apps.AppsAsyncService;
import com.alibaba.dashscope.v3.resources.apps.AppsService;
import com.alibaba.dashscope.v3.resources.audio.AudioAsyncService;
import com.alibaba.dashscope.v3.resources.audio.AudioService;
import com.alibaba.dashscope.v3.resources.chat.ChatAsyncService;
import com.alibaba.dashscope.v3.resources.chat.ChatService;
import com.alibaba.dashscope.v3.resources.decision.DecisionAsyncService;
import com.alibaba.dashscope.v3.resources.decision.DecisionService;
import com.alibaba.dashscope.v3.resources.deployments.DeploymentsAsyncService;
import com.alibaba.dashscope.v3.resources.deployments.DeploymentsService;
import com.alibaba.dashscope.v3.resources.embeddings.EmbeddingAsyncService;
import com.alibaba.dashscope.v3.resources.embeddings.EmbeddingService;
import com.alibaba.dashscope.v3.resources.files.FilesAsyncService;
import com.alibaba.dashscope.v3.resources.files.FilesService;
import com.alibaba.dashscope.v3.resources.finetunes.FineTunesAsyncService;
import com.alibaba.dashscope.v3.resources.finetunes.FineTunesService;
import com.alibaba.dashscope.v3.resources.images.ImagesAsyncService;
import com.alibaba.dashscope.v3.resources.images.ImagesService;
import com.alibaba.dashscope.v3.resources.models.ModelsAsyncService;
import com.alibaba.dashscope.v3.resources.models.ModelsService;
import com.alibaba.dashscope.v3.resources.multimodal.MultimodalAsyncService;
import com.alibaba.dashscope.v3.resources.multimodal.MultimodalService;
import com.alibaba.dashscope.v3.resources.rerank.RerankAsyncService;
import com.alibaba.dashscope.v3.resources.rerank.RerankService;
import com.alibaba.dashscope.v3.resources.tingwu.TingWuAsyncService;
import com.alibaba.dashscope.v3.resources.tingwu.TingWuService;
import com.alibaba.dashscope.v3.resources.tokenization.TokenizationAsyncService;
import com.alibaba.dashscope.v3.resources.tokenization.TokenizationService;
import com.alibaba.dashscope.v3.resources.videos.VideosAsyncService;
import com.alibaba.dashscope.v3.resources.videos.VideosService;
import com.alibaba.dashscope.v3.transport.HttpTransport;
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
  private final MultimodalAsyncService multimodal;
  private final AudioAsyncService audio;
  private final AppsAsyncService apps;
  private final DecisionAsyncService decision;
  private final ImagesAsyncService images;
  private final VideosAsyncService videos;
  private final FilesAsyncService files;
  private final ModelsAsyncService models;
  private final FineTunesAsyncService fineTunes;
  private final DeploymentsAsyncService deployments;
  private final TingWuAsyncService tingwu;
  private final TokenizationAsyncService tokenization;
  private volatile AgentStudioClient agentStudio;

  private DashScopeAsyncClient(
      ClientOptions options, HttpTransport transport, ExecutorService executor, boolean ownsExecutor) {
    this.options = options;
    this.transport = transport;
    this.executor = executor;
    this.ownsExecutor = ownsExecutor;
    this.chat = new ChatAsyncService(new ChatService(transport), executor);
    this.embeddings = new EmbeddingAsyncService(new EmbeddingService(transport), executor);
    this.rerank = new RerankAsyncService(new RerankService(transport), executor);
    this.multimodal = new MultimodalAsyncService(new MultimodalService(transport), executor);
    this.audio = new AudioAsyncService(new AudioService(transport), executor);
    this.apps = new AppsAsyncService(new AppsService(transport), executor);
    this.decision = new DecisionAsyncService(new DecisionService(transport), executor);
    this.images = new ImagesAsyncService(new ImagesService(transport), executor);
    this.videos = new VideosAsyncService(new VideosService(transport), executor);
    this.files = new FilesAsyncService(new FilesService(transport), executor);
    this.models = new ModelsAsyncService(new ModelsService(transport), executor);
    this.fineTunes = new FineTunesAsyncService(new FineTunesService(transport), executor);
    this.deployments = new DeploymentsAsyncService(new DeploymentsService(transport), executor);
    this.tingwu = new TingWuAsyncService(new TingWuService(transport), executor);
    this.tokenization =
        new TokenizationAsyncService(new TokenizationService(transport), executor);
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

  public MultimodalAsyncService multimodal() {
    return multimodal;
  }

  public AppsAsyncService apps() {
    return apps;
  }

  public DecisionAsyncService decision() {
    return decision;
  }

  public ImagesAsyncService images() {
    return images;
  }

  public VideosAsyncService videos() {
    return videos;
  }

  /** File management: upload/list/get/delete. */
  public FilesAsyncService files() {
    return files;
  }

  /** Model catalog: list/get. */
  public ModelsAsyncService models() {
    return models;
  }

  /** Fine-tuning jobs: create/get/list/cancel/delete/logs/streamEvents/wait. */
  public FineTunesAsyncService fineTunes() {
    return fineTunes;
  }

  /** Model deployments: create/get/list/scale/delete/streamEvents. */
  public DeploymentsAsyncService deployments() {
    return deployments;
  }

  /** TingWu offline jobs only (realtime sessions stay sync-only, Python v2 parity). */
  public TingWuAsyncService tingwu() {
    return tingwu;
  }

  /** Server-side token counting. */
  public TokenizationAsyncService tokenization() {
    return tokenization;
  }

  /**
   * AgentStudio management plane (design §7: single shared copy — returns the 2.x {@code
   * agentstudio} client type used by both generations). The same client type as on {@link
   * DashScopeClient#agentStudio()} — it carries native async variants. Created lazily; it keeps
   * its own 2.x-style exception style and the 3.0 {@code baseUrl} does not apply to it.
   */
  public AgentStudioClient agentStudio() {
    AgentStudioClient c = agentStudio;
    if (c == null) {
      synchronized (this) {
        c = agentStudio;
        if (c == null) {
          ResolvedConfig config = transport.config();
          c =
              new AgentStudioClient(
                  config.getApiKey(), null, config.getWorkspace(), config.getRegion(), null);
          agentStudio = c;
        }
      }
    }
    return c;
  }

  /** Audio: asr batch transcription only (ws duplex surfaces stay on the sync client). */
  public AudioAsyncService audio() {
    return audio;
  }

  /** Derive a client with extra overrides, sharing this client's pool and executor. */
  public DashScopeAsyncClient withOptions(ClientOptions overrides) {
    ClientOptions merged = DashScopeClient.merge(options, overrides);
    return new DashScopeAsyncClient(
        merged, transport.derive(ConfigResolver.resolve(merged)), executor, false);
  }

  @Override
  public void close() {
    AgentStudioClient c = agentStudio;
    if (c != null) {
      c.close();
    }
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
