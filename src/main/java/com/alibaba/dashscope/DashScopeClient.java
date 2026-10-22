// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.agentstudio.AgentStudioClient;
import com.alibaba.dashscope.v3.config.ConfigResolver;
import com.alibaba.dashscope.v3.config.ResolvedConfig;
import com.alibaba.dashscope.v3.resources.apps.AppsService;
import com.alibaba.dashscope.v3.resources.audio.AudioService;
import com.alibaba.dashscope.v3.resources.chat.ChatService;
import com.alibaba.dashscope.v3.resources.decision.DecisionService;
import com.alibaba.dashscope.v3.resources.deployments.DeploymentsService;
import com.alibaba.dashscope.v3.resources.embeddings.EmbeddingService;
import com.alibaba.dashscope.v3.resources.files.FilesService;
import com.alibaba.dashscope.v3.resources.finetunes.FineTunesService;
import com.alibaba.dashscope.v3.resources.images.ImagesService;
import com.alibaba.dashscope.v3.resources.models.ModelsService;
import com.alibaba.dashscope.v3.resources.multimodal.MultimodalService;
import com.alibaba.dashscope.v3.resources.rerank.RerankService;
import com.alibaba.dashscope.v3.resources.tingwu.TingWuService;
import com.alibaba.dashscope.v3.resources.tokenization.TokenizationService;
import com.alibaba.dashscope.v3.resources.videos.VideosService;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.ws.WsConnection;
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
  private final MultimodalService multimodal;
  private final AudioService audio;
  private final AppsService apps;
  private final DecisionService decision;
  private final ImagesService images;
  private final VideosService videos;
  private final FilesService files;
  private final ModelsService models;
  private final FineTunesService fineTunes;
  private final DeploymentsService deployments;
  private final TingWuService tingwu;
  private final TokenizationService tokenization;
  private volatile AgentStudioClient agentStudio;

  private DashScopeClient(ClientOptions options, HttpTransport transport) {
    this.options = options;
    this.config = transport.config();
    this.transport = transport;
    this.chat = new ChatService(transport);
    this.embeddings = new EmbeddingService(transport);
    this.rerank = new RerankService(transport);
    this.multimodal = new MultimodalService(transport);
    this.audio = new AudioService(transport);
    this.apps = new AppsService(transport);
    this.decision = new DecisionService(transport);
    this.images = new ImagesService(transport);
    this.videos = new VideosService(transport);
    this.files = new FilesService(transport);
    this.models = new ModelsService(transport);
    this.fineTunes = new FineTunesService(transport);
    this.deployments = new DeploymentsService(transport);
    this.tingwu = new TingWuService(transport);
    this.tokenization = new TokenizationService(transport);
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

  public MultimodalService multimodal() {
    return multimodal;
  }

  public AudioService audio() {
    return audio;
  }

  /** Bailian applications: {@code apps().completions().create(...)}. */
  public AppsService apps() {
    return apps;
  }

  /** System One decision model: typed questions over a business state, one round trip. */
  public DecisionService decision() {
    return decision;
  }

  /** Text-to-image: {@code generate} blocking sugar + {@code jobs()} task handle. */
  public ImagesService images() {
    return images;
  }

  /** Video generation: {@code generate} blocking sugar + {@code jobs()} task handle. */
  public VideosService videos() {
    return videos;
  }

  /** File management: upload/list/get/delete. */
  public FilesService files() {
    return files;
  }

  /** Model catalog: list/get. */
  public ModelsService models() {
    return models;
  }

  /** Fine-tuning jobs: create/get/list/cancel/delete/logs/streamEvents/wait. */
  public FineTunesService fineTunes() {
    return fineTunes;
  }

  /** Model deployments: create/get/list/scale/delete/streamEvents. */
  public DeploymentsService deployments() {
    return deployments;
  }

  /** TingWu: offline summarization jobs + realtime understanding. */
  public TingWuService tingwu() {
    return tingwu;
  }

  /**
   * Server-side token counting. For the local BPE tokenizer see {@link
   * com.alibaba.dashscope.v3.tokenization.TokenizerFactory}.
   */
  public TokenizationService tokenization() {
    return tokenization;
  }

  /**
   * AgentStudio management plane (design §7: single shared copy — the 2.x {@code agentstudio}
   * package is the canonical implementation for both generations; not deprecated, untouched by
   * the v3 move and by the 4.0 cleanup). Created lazily — endpoint resolution requires a
   * workspace or {@code DASHSCOPE_AGENTSTUDIO_URL}, and the subarea allocates its own HTTP
   * clients. It keeps its own 2.x-style exception style; the 3.0 {@code baseUrl} does not apply
   * to it.
   */
  public AgentStudioClient agentStudio() {
    AgentStudioClient c = agentStudio;
    if (c == null) {
      synchronized (this) {
        c = agentStudio;
        if (c == null) {
          c =
              new AgentStudioClient(
                  config.getApiKey(), null, config.getWorkspace(), config.getRegion(), null);
          agentStudio = c;
        }
      }
    }
    return c;
  }

  /** The resolved configuration (config chain applied). */
  public ResolvedConfig config() {
    return config;
  }

  /**
   * Open a shared WebSocket connection multiplexing many task-protocol tasks (audio design §7).
   * The caller owns it and must close it; pass it to {@code asr().recognize(params, connection)}
   * / {@code tts().connect(params, connection)} to reuse the handshake.
   */
  public WsConnection wsConnection() {
    return wsConnection(RequestOptions.NONE);
  }

  public WsConnection wsConnection(RequestOptions options) {
    return new WsConnection(transport, config.getWsBaseUrl(), "audio", options);
  }

  /** Derive a client with extra overrides, sharing this client's connection pool. */
  public DashScopeClient withOptions(ClientOptions overrides) {
    ClientOptions merged = merge(options, overrides);
    return new DashScopeClient(merged, transport.derive(ConfigResolver.resolve(merged)));
  }

  @Override
  public void close() {
    AgentStudioClient c = agentStudio;
    if (c != null) {
      c.close();
    }
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
