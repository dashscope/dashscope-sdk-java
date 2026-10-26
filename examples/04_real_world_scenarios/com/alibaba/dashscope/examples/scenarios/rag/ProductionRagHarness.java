// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.scenarios.rag;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ContentFilterException;
import com.alibaba.dashscope.errors.DashScopeException;
import com.alibaba.dashscope.errors.RateLimitException;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.completions.Delta;
import com.alibaba.dashscope.types.embeddings.Embedding;
import com.alibaba.dashscope.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.types.embeddings.EmbeddingResponse;
import com.alibaba.dashscope.types.messages.Message;
import com.alibaba.dashscope.types.rerank.RerankCreateParams;
import com.alibaba.dashscope.types.rerank.RerankResponse;
import com.alibaba.dashscope.types.rerank.RerankResult;
import com.alibaba.dashscope.types.tokenization.TokenizationCountParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Real-world scenario: a production-ready RAG harness.
 *
 * <p>This is not "how to call embeddings + chat". It is an industrial-grade
 * retrieval-augmented-generation engine that stitches the four things a real knowledge-base service
 * must get right into one runnable blueprint:
 *
 * <ol>
 *   <li><b>Streaming semantic retrieval</b> — embed once, cosine top-k, then a cross-encoder {@code
 *       client.rerank()} pass for precision.
 *   <li><b>Local token watermark</b> — every retrieved passage is measured with {@code
 *       client.tokenization().count} and only the passages that fit a local token budget reach the
 *       model, so a fat context can never blow the window or the bill.
 *   <li><b>Rigid resilience</b> — the SDK retries transient 429/5xx <i>within</i> a region for
 *       normal calls ({@code maxRetries}), but a STREAMING call is NOT retried by the SDK (streams
 *       are tagged no-retry: the status is checked once when the stream opens). So the harness owns
 *       exponential backoff for the streaming generate step, and layers a per-region circuit
 *       breaker + cross-region failover on top.
 *   <li><b>Graceful degradation</b> — 429 / content-filter / total-outage are caught and turned
 *       into a clean degraded answer instead of a stack trace.
 * </ol>
 *
 * <p>It runs <b>fully offline and deterministically by default</b> through in-process JDK {@link
 * HttpServer} instances (one per mock region) that serve canned embeddings / rerank / tokenizer /
 * chat envelopes and inject faults on demand, so you can <i>see</i> the retry, failover and
 * watermark logic work without spending a token. Set {@code RAG_HARNESS_LIVE=1} to point the exact
 * same engine at the real gateway with {@code DASHSCOPE_API_KEY}.
 *
 * <p>Translation of the Python blueprint example
 * 04_real_world_scenarios/production_ready_rag_harness.py.
 */
public final class ProductionRagHarness {

  // ---- configuration (override via env) ----
  private static final String EMBED_MODEL = envOr("DASHSCOPE_EMBED_MODEL", "text-embedding-v4");
  private static final String RERANK_MODEL = envOr("DASHSCOPE_RERANK_MODEL", "gte-rerank-v2");
  private static final String CHAT_MODEL = envOr("DASHSCOPE_MODEL", "qwen3.7-plus");
  private static final boolean LIVE = "1".equals(System.getenv("RAG_HARNESS_LIVE"));

  /**
   * Fail over across these regions, in order (offline demo only; real cross-region use needs a
   * region-specific key + workspace).
   */
  private static final List<String> REGIONS = Arrays.asList("cn-beijing", "ap-southeast-1");

  /** Candidates pulled from the vector store before rerank. */
  private static final int TOP_K = 4;
  /**
   * Local token budget for the retrieved passages. In production derive it from the model's max
   * context minus the instruction and a reserved answer allowance; here it is a fixed watermark
   * that visibly trims.
   */
  private static final int CONTEXT_TOKEN_BUDGET = 140;
  /** Mock embedding dimensionality. */
  private static final int EMBED_DIM = 64;
  /** Harness-owned retries for the streaming generate step. */
  private static final int GEN_RETRIES = 2;

  private static final String QUERY = "DashScope 支持哪些模型能力？";

  /** A tiny stand-in knowledge base; load chunks from files or a DB in prod. */
  private static final List<String> CORPUS =
      Arrays.asList(
          "DashScope 是阿里云百炼的模型服务 SDK，支持文本生成、向量、重排等能力。",
          "文本生成通过 chat.completions 调用 qwen 系列大模型，支持流式与非流式。",
          "文本向量（embedding）把句子映射为定长浮点向量，用于语义检索与聚类。",
          "重排（rerank）用交叉编码器对候选文档按与查询的相关性重新打分。",
          "百炼还提供图片生成、语音合成与识别、以及微调等多模态能力。",
          "杭州是浙江省的省会，位于中国东南沿海，与本项目无关。");

  private ProductionRagHarness() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  // ===================== local vector & similarity utils =====================

  /** Cosine similarity of two equal-length float vectors. */
  private static double cosine(List<Double> a, List<Double> b) {
    double dot = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < a.size(); i++) {
      double x = a.get(i);
      double y = b.get(i);
      dot += x * y;
      normA += x * x;
      normB += y * y;
    }
    if (normA == 0.0 || normB == 0.0) {
      return 0.0;
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  /**
   * Deterministic bag-of-characters pseudo-embedding for offline use. Real deployments get these
   * from {@code client.embeddings()}; the mock needs a stable vector so cosine retrieval is
   * meaningful without a network.
   */
  static List<Double> mockVector(String text) {
    double[] vec = new double[EMBED_DIM];
    for (int i = 0; i < text.length(); i++) {
      vec[text.charAt(i) % EMBED_DIM] += 1.0;
    }
    double norm = 0.0;
    for (double v : vec) {
      norm += v * v;
    }
    norm = Math.sqrt(norm);
    if (norm == 0.0) {
      norm = 1.0;
    }
    List<Double> out = new ArrayList<>(EMBED_DIM);
    for (double v : vec) {
      out.add(v / norm);
    }
    return out;
  }

  /** 1 CJK char ~= 1 token: a safe upper-bound estimate for the mock. */
  static int estimateTokens(String text) {
    return Math.max(1, text.length());
  }

  // ===================== circuit breaker & resilience =====================

  enum BreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

  /** Minimal per-region circuit breaker (CLOSED/OPEN/HALF_OPEN). */
  static final class CircuitBreaker {
    private final int failureThreshold;
    private final long resetNanos;
    private BreakerState state = BreakerState.CLOSED;
    private int failureCount;
    private long openedAtNanos;

    CircuitBreaker(int failureThreshold, long resetNanos) {
      this.failureThreshold = failureThreshold;
      this.resetNanos = resetNanos;
    }

    /** Whether a request may be attempted right now. */
    synchronized boolean allow() {
      if (state == BreakerState.OPEN) {
        if (System.nanoTime() - openedAtNanos >= resetNanos) {
          state = BreakerState.HALF_OPEN;
          return true;
        }
        return false;
      }
      return true;
    }

    synchronized void recordSuccess() {
      failureCount = 0;
      state = BreakerState.CLOSED;
    }

    synchronized void recordFailure() {
      failureCount++;
      if (state == BreakerState.HALF_OPEN || failureCount >= failureThreshold) {
        state = BreakerState.OPEN;
        openedAtNanos = System.nanoTime();
      }
    }

    synchronized BreakerState state() {
      return state;
    }
  }

  /** Connection errors and 5xx / 429 are worth retrying or failing over. */
  static boolean isRetryable(DashScopeException exc) {
    if (exc instanceof ApiConnectionException) {
      return true;
    }
    Integer status = exc.getStatusCode();
    return status != null && (status >= 500 || status == 429);
  }

  /**
   * Exponential backoff for calls the SDK will NOT retry itself.
   *
   * <p>Streaming requests bypass the transport's retry loop, so the harness owns this. {@code
   * baseDelaySeconds=0} keeps the offline demo instant; use ~0.5 in production and honor a server
   * Retry-After when present.
   */
  static <T> T retryWithBackoff(Supplier<T> fn, int retries, double baseDelaySeconds) {
    int attempt = 0;
    while (true) {
      try {
        return fn.get();
      } catch (DashScopeException exc) {
        if (!isRetryable(exc) || attempt >= retries) {
          throw exc;
        }
        double delay = baseDelaySeconds * Math.pow(2.0, attempt);
        System.out.printf(
            "      · generate retry %d/%d after %s (backoff %.2fs)%n",
            attempt + 1, retries, exc.getClass().getSimpleName(), delay);
        if (delay > 0.0) {
          try {
            Thread.sleep((long) (delay * 1000.0));
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw exc;
          }
        }
        attempt++;
      }
    }
  }

  /**
   * Run each pipeline step against the first healthy region.
   *
   * <p>Within a region the SDK's own {@code maxRetries} absorbs transient errors for non-streaming
   * calls; across regions this adds a circuit breaker so a whole-region outage fails over instead
   * of falling over.
   */
  static final class ResilientGateway implements AutoCloseable {
    private final List<String> regions;
    private final Function<String, DashScopeClient> factory;
    private final Map<String, CircuitBreaker> breakers = new HashMap<>();
    private final Map<String, DashScopeClient> clients = new HashMap<>();

    ResilientGateway(List<String> regions, Function<String, DashScopeClient> factory) {
      this.regions = new ArrayList<>(regions);
      this.factory = factory;
      for (String region : regions) {
        breakers.put(region, new CircuitBreaker(2, 30_000_000_000L));
      }
    }

    private DashScopeClient client(String region) {
      return clients.computeIfAbsent(region, factory);
    }

    /** Return (region, result) from the first healthy region. */
    <T> Map.Entry<String, T> run(String op, Function<DashScopeClient, T> fn, boolean quiet) {
      DashScopeException lastExc = null;
      for (String region : regions) {
        CircuitBreaker breaker = breakers.get(region);
        if (!breaker.allow()) {
          if (!quiet) {
            System.out.println("    [" + region + "] circuit OPEN -> skip " + op);
          }
          continue;
        }
        try {
          T result = fn.apply(client(region));
          breaker.recordSuccess();
          return new AbstractMap.SimpleImmutableEntry<>(region, result);
        } catch (DashScopeException exc) {
          lastExc = exc;
          if (!isRetryable(exc)) {
            throw exc; // a 4xx is a real bug; never mask it
          }
          breaker.recordFailure();
          if (!quiet) {
            System.out.println(
                "    ["
                    + region
                    + "] "
                    + exc.getClass().getSimpleName()
                    + " status="
                    + exc.getStatusCode()
                    + " on "
                    + op
                    + " -> circuit "
                    + breaker.state()
                    + "; failing over");
          }
        }
      }
      throw new ApiConnectionException("all regions failed for " + op + ": " + lastExc, lastExc);
    }

    @Override
    public void close() {
      for (DashScopeClient client : clients.values()) {
        client.close();
      }
      clients.clear();
    }
  }

  // ===================== retrieve / watermark / generate =====================

  /** In-memory vector DB (swap for Milvus / AnalyticDB / pgvector). */
  static final class LocalVectorStore {
    private final ResilientGateway gateway;
    private final String embedModel;
    private final List<String> texts = new ArrayList<>();
    private final List<List<Double>> vectors = new ArrayList<>();

    LocalVectorStore(ResilientGateway gateway, String embedModel) {
      this.gateway = gateway;
      this.embedModel = embedModel;
    }

    void index(List<String> corpus) {
      Map.Entry<String, EmbeddingResponse> outcome =
          gateway.run(
              "embed-index",
              client ->
                  client
                      .embeddings()
                      .create(
                          EmbeddingCreateParams.builder().model(embedModel).texts(corpus).build()),
              false);
      List<Embedding> ordered = new ArrayList<>(outcome.getValue().getEmbeddings());
      ordered.sort(Comparator.comparingInt(Embedding::getIndex));
      texts.clear();
      vectors.clear();
      texts.addAll(corpus);
      for (Embedding item : ordered) {
        vectors.add(item.getEmbedding());
      }
    }

    /** Return the top_k passages as (text, score) by cosine. */
    List<Map.Entry<String, Double>> search(String query, int topK) {
      Map.Entry<String, EmbeddingResponse> outcome =
          gateway.run(
              "embed-query",
              client ->
                  client
                      .embeddings()
                      .create(
                          EmbeddingCreateParams.builder().model(embedModel).text(query).build()),
              false);
      List<Double> queryVec = outcome.getValue().getEmbeddings().get(0).getEmbedding();
      List<Integer> order = new ArrayList<>();
      for (int i = 0; i < vectors.size(); i++) {
        order.add(i);
      }
      order.sort(
          (a, b) ->
              Double.compare(cosine(queryVec, vectors.get(b)), cosine(queryVec, vectors.get(a))));
      List<Map.Entry<String, Double>> hits = new ArrayList<>();
      for (int i = 0; i < Math.min(topK, order.size()); i++) {
        int idx = order.get(i);
        hits.add(
            new AbstractMap.SimpleImmutableEntry<>(
                texts.get(idx), cosine(queryVec, vectors.get(idx))));
      }
      return hits;
    }
  }

  /** Local token budget: only passages that fit reach the model. */
  static final class TokenWatermark {
    private final ResilientGateway gateway;
    private final String model;
    private final int budget;

    TokenWatermark(ResilientGateway gateway, String model, int budget) {
      this.gateway = gateway;
      this.model = model;
      this.budget = budget;
    }

    int count(String text) {
      // quiet: tokenizing every passage is high-frequency, so it does not repeat the failover
      // trace the embed/rerank/chat steps show.
      Map.Entry<String, Integer> outcome =
          gateway.run(
              "tokenize",
              client ->
                  client
                      .tokenization()
                      .count(TokenizationCountParams.builder().model(model).prompt(text).build())
                      .getInputTokens(),
              true);
      return outcome.getValue();
    }

    /** Greedily keep ranked passages under budget. */
    WatermarkFit fit(List<String> passages) {
      WatermarkFit fit = new WatermarkFit();
      for (String passage : passages) {
        int tokens = count(passage);
        if (fit.used + tokens > budget) {
          break;
        }
        fit.kept.add(passage);
        fit.used += tokens;
      }
      fit.dropped = passages.size() - fit.kept.size();
      return fit;
    }
  }

  /** Result of {@link TokenWatermark#fit}: kept passages, tokens used, passages dropped. */
  static final class WatermarkFit {
    final List<String> kept = new ArrayList<>();
    int used;
    int dropped;
  }

  /** Structured result so callers can log/observe, not just print. */
  static final class RagAnswer {
    final String answer;
    final List<String> citations;
    final String servedRegion;
    final Integer usageTotal;
    final String requestId;
    final int kept;
    final int dropped;
    final boolean degraded;
    final String errorCode;

    RagAnswer(
        String answer,
        List<String> citations,
        String servedRegion,
        Integer usageTotal,
        String requestId,
        int kept,
        int dropped,
        boolean degraded,
        String errorCode) {
      this.answer = answer;
      this.citations = citations == null ? Collections.emptyList() : citations;
      this.servedRegion = servedRegion;
      this.usageTotal = usageTotal;
      this.requestId = requestId;
      this.kept = kept;
      this.dropped = dropped;
      this.degraded = degraded;
      this.errorCode = errorCode;
    }
  }

  /** Folded result of consuming one chat stream. */
  private static final class StreamedAnswer {
    final String text;
    final Integer usageTotal;
    final String requestId;

    StreamedAnswer(String text, Integer usageTotal, String requestId) {
      this.text = text;
      this.usageTotal = usageTotal;
      this.requestId = requestId;
    }
  }

  /** Embed -> rerank -> token-watermark -> resilient streaming chat. */
  static final class ProductionRagEngine {
    private final ResilientGateway gateway;
    private final LocalVectorStore store;
    private final TokenWatermark watermark;
    private final double baseDelay;

    ProductionRagEngine(ResilientGateway gateway, double baseDelay) {
      this.gateway = gateway;
      this.store = new LocalVectorStore(gateway, EMBED_MODEL);
      this.watermark = new TokenWatermark(gateway, CHAT_MODEL, CONTEXT_TOKEN_BUDGET);
      this.baseDelay = baseDelay;
    }

    void ingest(List<String> corpus) {
      store.index(corpus);
    }

    /** Close the gateway (and every region client it created). */
    void close() {
      gateway.close();
    }

    private List<String> rerank(String query, List<String> candidates) {
      Map.Entry<String, RerankResponse> outcome =
          gateway.run(
              "rerank",
              client ->
                  client
                      .rerank()
                      .create(
                          RerankCreateParams.builder()
                              .model(RERANK_MODEL)
                              .query(query)
                              .documents(candidates)
                              .topN(candidates.size())
                              .returnDocuments(true)
                              .build()),
              false);
      List<String> ranked = new ArrayList<>();
      for (RerankResult result : outcome.getValue().getResults()) {
        if (result.getDocument() != null) {
          ranked.add(result.getDocument().getText());
        }
      }
      return ranked;
    }

    private ResponseStream<ChatCompletionChunk> openStream(DashScopeClient client, String prompt) {
      // The SDK does not retry streams, so the harness backs off here.
      return retryWithBackoff(
          () ->
              client
                  .chat()
                  .completions()
                  .createStreaming(
                      ChatCompletionCreateParams.builder()
                          .model(CHAT_MODEL)
                          .messages(Collections.singletonList(Message.user(prompt)))
                          .build()),
          GEN_RETRIES,
          baseDelay);
    }

    private Map.Entry<String, StreamedAnswer> generate(String prompt) {
      Map.Entry<String, ResponseStream<ChatCompletionChunk>> opened =
          gateway.run("chat", client -> openStream(client, prompt), false);
      return new AbstractMap.SimpleImmutableEntry<>(
          opened.getKey(), consumeStream(opened.getValue()));
    }

    /**
     * Run the full pipeline and return a {@link RagAnswer}.
     *
     * <p>Every stage (retrieval, rerank, watermark, generation) sits inside one guard, so a failure
     * anywhere — not just generation — degrades gracefully instead of raising to the caller.
     */
    RagAnswer answer(String query) {
      try {
        return answerOrRaise(query);
      } catch (RateLimitException exc) {
        return degrade("系统繁忙（触发限流），请稍后重试。", exc);
      } catch (ContentFilterException exc) {
        return degrade("内容未通过安全审核，无法作答。", exc);
      } catch (DashScopeException exc) {
        return degrade("检索/生成服务暂不可用，已降级返回。", exc);
      }
    }

    private RagAnswer answerOrRaise(String query) {
      List<Map.Entry<String, Double>> hits = store.search(query, TOP_K);
      List<String> candidates = new ArrayList<>();
      for (Map.Entry<String, Double> hit : hits) {
        candidates.add(hit.getKey());
      }
      List<String> ranked = rerank(query, candidates);
      WatermarkFit fit = watermark.fit(ranked);
      System.out.printf(
          "    token watermark: kept %d passage(s), %d/%d tokens, dropped %d%n",
          fit.kept.size(), fit.used, CONTEXT_TOKEN_BUDGET, fit.dropped);
      if (fit.kept.isEmpty()) {
        return new RagAnswer("资料不足，无法回答该问题。", null, null, null, null, 0, fit.dropped, true, null);
      }
      String prompt = buildPrompt(fit.kept, query);
      Map.Entry<String, StreamedAnswer> generated = generate(prompt);
      StreamedAnswer value = generated.getValue();
      return new RagAnswer(
          value.text,
          fit.kept,
          generated.getKey(),
          value.usageTotal,
          value.requestId,
          fit.kept.size(),
          fit.dropped,
          false,
          null);
    }

    private static RagAnswer degrade(String message, DashScopeException exc) {
      String code = exc.getCode();
      System.out.println(
          "    graceful degrade: " + exc.getClass().getSimpleName() + " code=" + code);
      return new RagAnswer(message, null, null, null, null, 0, 0, true, code);
    }
  }

  private static String buildPrompt(List<String> passages, String query) {
    StringBuilder context = new StringBuilder();
    for (int i = 0; i < passages.size(); i++) {
      context.append('[').append(i).append("] ").append(passages.get(i)).append('\n');
    }
    return "你是企业知识库助手。仅依据下面的资料回答问题，并在句末用 [n] 标注引用编号；若资料不足，直接回答：资料不足。\n"
        + "资料：\n"
        + context
        + "\n问题："
        + query;
  }

  /**
   * Print deltas live and capture usage without a second iteration.
   *
   * <p>A stream is a one-shot iterator: reading content and then asking for a final message would
   * see an empty tail. Iterating once and folding content + usage together is the correct merge
   * (the usage counter arrives on the final chunk, not the first).
   */
  private static StreamedAnswer consumeStream(ResponseStream<ChatCompletionChunk> stream) {
    StringBuilder text = new StringBuilder();
    Integer usageTotal = null;
    String requestId = null;
    System.out.print("客服回答: ");
    System.out.flush();
    try (ResponseStream<ChatCompletionChunk> closeable = stream) {
      for (ChatCompletionChunk chunk : closeable) {
        Delta delta = chunk.delta();
        if (delta != null && delta.getContent() != null && !delta.getContent().isEmpty()) {
          text.append(delta.getContent());
          System.out.print(delta.getContent());
          System.out.flush();
        }
        if (chunk.getUsage() != null) {
          usageTotal = chunk.getUsage().getTotalTokens();
        }
        if (chunk.getRequestId() != null && !chunk.getRequestId().isEmpty()) {
          requestId = chunk.getRequestId();
        }
      }
    }
    System.out.println();
    return new StreamedAnswer(text.toString(), usageTotal, requestId);
  }

  // ===================== offline mock gateway w/ faults =====================

  /**
   * Per-region mock servers routing by path, serving canned envelopes, injecting faults on demand.
   * A region outage takes down every endpoint together, so the breaker accumulates failures across
   * the whole pipeline.
   */
  static final class MockGateway {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    final Set<String> downRegions = new HashSet<>();
    int rateLimitNextChat;
    int injected429;

    private final Map<String, HttpServer> servers = new HashMap<>();

    /** Start one mock server for a region; returns its base URL for the SDK client. */
    String startRegion(String region) throws IOException {
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/", exchange -> route(region, exchange));
      server.start();
      servers.put(region, server);
      return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1";
    }

    void stopAll() {
      for (HttpServer server : servers.values()) {
        server.stop(0);
      }
      servers.clear();
    }

    private void route(String region, HttpExchange exchange) throws IOException {
      try {
        byte[] requestBytes = readAll(exchange.getRequestBody());
        if (downRegions.contains(region)) {
          respondFault(exchange, 503, "ServiceUnavailable", "mocked regional outage in " + region);
          return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/tokenizer")) {
          handleTokenizer(exchange, requestBytes);
        } else if (path.contains("embeddings")) {
          handleEmbeddings(exchange, requestBytes);
        } else if (path.contains("rerank")) {
          handleRerank(exchange, requestBytes);
        } else {
          handleChat(region, exchange, requestBytes);
        }
      } finally {
        exchange.close();
      }
    }

    /** Retry-After: 0 keeps the SDK/harness backoff instant in the demo. */
    private static void respondFault(HttpExchange exchange, int status, String code, String message)
        throws IOException {
      ObjectNode body = MAPPER.createObjectNode();
      body.put("code", code);
      body.put("message", message);
      exchange.getResponseHeaders().set("Retry-After", "0");
      respond(exchange, status, body);
    }

    private static void handleEmbeddings(HttpExchange exchange, byte[] requestBytes)
        throws IOException {
      JsonNode body = MAPPER.readTree(requestBytes);
      ArrayNode embeddings = MAPPER.createArrayNode();
      int totalTokens = 0;
      int index = 0;
      for (JsonNode textNode : body.path("input").path("texts")) {
        String text = textNode.asText();
        totalTokens += text.length();
        ObjectNode item = embeddings.addObject();
        item.put("index", index++);
        item.set("embedding", MAPPER.valueToTree(mockVector(text)));
      }
      ObjectNode root = MAPPER.createObjectNode();
      ObjectNode output = root.putObject("output");
      output.set("embeddings", embeddings);
      root.putObject("usage").put("total_tokens", totalTokens);
      root.put("request_id", "mock-embed");
      respond(exchange, 200, root);
    }

    private static void handleRerank(HttpExchange exchange, byte[] requestBytes)
        throws IOException {
      JsonNode body = MAPPER.readTree(requestBytes);
      String query = body.path("input").path("query").asText();
      List<Double> queryVec = mockVector(query);
      List<double[]> scored = new ArrayList<>();
      List<String> docs = new ArrayList<>();
      int index = 0;
      for (JsonNode docNode : body.path("input").path("documents")) {
        String doc = docNode.asText();
        docs.add(doc);
        scored.add(new double[] {cosine(queryVec, mockVector(doc)), index++});
      }
      scored.sort((a, b) -> Double.compare(b[0], a[0]));
      ArrayNode results = MAPPER.createArrayNode();
      for (double[] pair : scored) {
        ObjectNode item = results.addObject();
        item.put("index", (int) pair[1]);
        item.put("relevance_score", Math.round(pair[0] * 10000.0) / 10000.0);
        item.putObject("document").put("text", docs.get((int) pair[1]));
      }
      ObjectNode root = MAPPER.createObjectNode();
      root.putObject("output").set("results", results);
      root.putObject("usage").put("total_tokens", docs.size());
      root.put("request_id", "mock-rerank");
      respond(exchange, 200, root);
    }

    private static void handleTokenizer(HttpExchange exchange, byte[] requestBytes)
        throws IOException {
      JsonNode body = MAPPER.readTree(requestBytes);
      int tokens = estimateTokens(body.path("input").path("prompt").asText(""));
      ObjectNode root = MAPPER.createObjectNode();
      ArrayNode ids = root.putObject("output").putArray("token_ids");
      for (int i = 0; i < tokens; i++) {
        ids.add(i);
      }
      root.putObject("usage").put("input_tokens", tokens);
      root.put("request_id", "mock-tokenizer");
      respond(exchange, 200, root);
    }

    private void handleChat(String region, HttpExchange exchange, byte[] requestBytes)
        throws IOException {
      if (rateLimitNextChat > 0) {
        rateLimitNextChat--;
        injected429++;
        respondFault(exchange, 429, "Throttling", "mocked rate limit");
        return;
      }
      JsonNode body = MAPPER.readTree(requestBytes);
      JsonNode messages = body.path("input").path("messages");
      String prompt =
          messages.isArray() && messages.size() > 0
              ? messages.get(messages.size() - 1).path("content").asText("")
              : "";
      String sse = exchange.getRequestHeaders().getFirst("X-DashScope-SSE");
      if ("enable".equals(sse)) {
        respondChatStream(exchange, prompt, region);
      } else {
        respondChatJson(exchange, prompt, region);
      }
    }

    private static ObjectNode chatEnvelope(String prompt, String region) {
      String text = cannedAnswer(prompt, region);
      ObjectNode root = MAPPER.createObjectNode();
      ObjectNode choice = root.putObject("output").putArray("choices").addObject();
      choice.put("finish_reason", "stop");
      ObjectNode message = choice.putObject("message");
      message.put("role", "assistant");
      message.put("content", text);
      ObjectNode usage = root.putObject("usage");
      usage.put("input_tokens", 20);
      usage.put("output_tokens", 10);
      usage.put("total_tokens", 30);
      root.put("request_id", "mock-chat-" + region);
      return root;
    }

    private static void respondChatJson(HttpExchange exchange, String prompt, String region)
        throws IOException {
      respond(exchange, 200, chatEnvelope(prompt, region));
    }

    private static void respondChatStream(HttpExchange exchange, String prompt, String region)
        throws IOException {
      String text = cannedAnswer(prompt, region);
      StringBuilder sse = new StringBuilder();
      int id = 0;
      for (int i = 0; i < text.length(); i += 12) {
        ObjectNode frame = MAPPER.createObjectNode();
        ObjectNode choice = frame.putObject("output").putArray("choices").addObject();
        choice.putNull("finish_reason");
        choice
            .putObject("message")
            .put("content", text.substring(i, Math.min(i + 12, text.length())));
        frame.put("request_id", "mock-chat-" + region);
        sse.append("id: ").append(++id).append('\n');
        sse.append("data: ").append(MAPPER.writeValueAsString(frame)).append("\n\n");
      }
      ObjectNode last = MAPPER.createObjectNode();
      ObjectNode lastChoice = last.putObject("output").putArray("choices").addObject();
      lastChoice.put("finish_reason", "stop");
      lastChoice.putObject("message").put("content", "");
      ObjectNode usage = last.putObject("usage");
      usage.put("input_tokens", 20);
      usage.put("output_tokens", text.length());
      usage.put("total_tokens", 20 + text.length());
      last.put("request_id", "mock-chat-" + region);
      sse.append("id: ").append(++id).append('\n');
      sse.append("data: ").append(MAPPER.writeValueAsString(last)).append("\n\n");
      byte[] bytes = sse.toString().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    }

    /**
     * Offline stand-in answer that reflects the passages it was given.
     *
     * <p>It reports how many [n] citations survived the token watermark, so the demo output makes
     * the trimming visible. Not real model output.
     */
    private static String cannedAnswer(String prompt, String region) {
      Matcher matcher = Pattern.compile("\\[(\\d+)]").matcher(prompt);
      StringBuilder cites = new StringBuilder();
      int count = 0;
      while (matcher.find()) {
        cites.append('[').append(matcher.group(1)).append(']');
        count++;
      }
      return "（离线 Mock 应答，节点 "
          + region
          + "）根据知识库中 "
          + count
          + " 段资料：DashScope 覆盖文本生成、向量与重排等能力。"
          + cites;
    }

    private static void respond(HttpExchange exchange, int status, JsonNode body)
        throws IOException {
      byte[] bytes = MAPPER.writeValueAsBytes(body);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(bytes);
      }
    }
  }

  // ===================== wiring & scenario demo =====================

  /** Build (engine, mock state or null) for offline or live mode. */
  private static Map.Entry<ProductionRagEngine, MockGateway> buildEngine() throws IOException {
    if (LIVE) {
      DashScopeClient live =
          DashScopeClient.builder().apiKey(ExampleSupport.requireApiKey()).maxRetries(2).build();
      ResilientGateway gateway =
          new ResilientGateway(Collections.singletonList("default"), region -> live);
      return new AbstractMap.SimpleImmutableEntry<>(new ProductionRagEngine(gateway, 0.5), null);
    }
    MockGateway mock = new MockGateway();
    Map<String, String> baseUrls = new LinkedHashMap<>();
    for (String region : REGIONS) {
      baseUrls.put(region, mock.startRegion(region));
    }
    ResilientGateway gateway =
        new ResilientGateway(
            REGIONS,
            region ->
                DashScopeClient.builder()
                    .apiKey("sk-mock")
                    .region(region)
                    .baseUrl(baseUrls.get(region))
                    .maxRetries(2)
                    .build());
    return new AbstractMap.SimpleImmutableEntry<>(new ProductionRagEngine(gateway, 0.0), mock);
  }

  private static void report(RagAnswer result) {
    System.out.println("    served by: " + result.servedRegion);
    System.out.println("    request_id: " + result.requestId + ", usage: " + result.usageTotal);
    System.out.println("    degraded: " + result.degraded + ", error_code: " + result.errorCode);
  }

  private static void scenarioRetrieval(ProductionRagEngine engine) {
    System.out.println("\n=== 场景 A：标准检索问答（召回->水位->精排->流式生成）===");
    System.out.println("用户提问: " + QUERY);
    report(engine.answer(QUERY));
  }

  private static void scenarioRateLimit(ProductionRagEngine engine, MockGateway mock) {
    System.out.println("\n=== 场景 B：429 限流，harness 指数退避自动吸收（流式不被 SDK 重试）===");
    mock.rateLimitNextChat = 1;
    report(engine.answer(QUERY));
    System.out.println("    注入并被吸收的 429 次数: " + mock.injected429);
  }

  private static void scenarioFailover(ProductionRagEngine engine, MockGateway mock) {
    System.out.println("\n=== 场景 C：主地域推理 503，熔断 + 跨地域灾备切换 ===");
    mock.downRegions.add(REGIONS.get(0));
    for (int i = 1; i <= 3; i++) {
      System.out.println("  -- 第 " + i + " 次问答 --");
      RagAnswer result = engine.answer(QUERY);
      System.out.println("    served by: " + result.servedRegion);
    }
  }

  private static void scenarioDegrade(ProductionRagEngine engine, MockGateway mock) {
    System.out.println("\n=== 场景 D：全部地域不可用，优雅降级而非抛栈 ===");
    mock.downRegions.addAll(REGIONS);
    RagAnswer result = engine.answer(QUERY);
    System.out.println("    降级返回: " + result.answer);
    System.out.println("    degraded=" + result.degraded + ", error_code=" + result.errorCode);
  }

  /** Ingest the corpus and run the resilience scenarios. */
  public static void main(String[] args) throws IOException {
    Map.Entry<ProductionRagEngine, MockGateway> built = buildEngine();
    ProductionRagEngine engine = built.getKey();
    MockGateway mock = built.getValue();
    try {
      System.out.println("模式: " + (LIVE ? "LIVE 真实网关" : "OFFLINE 本地 Mock"));
      engine.ingest(CORPUS);
      scenarioRetrieval(engine);
      if (mock == null) {
        System.out.println("\n(LIVE 模式跳过故障注入场景 B/C/D)");
        return;
      }
      scenarioRateLimit(engine, mock);
      scenarioFailover(engine, mock);
      scenarioDegrade(engine, mock);
    } finally {
      engine.close();
      if (mock != null) {
        mock.stopAll();
      }
    }
  }

  private static byte[] readAll(java.io.InputStream in) throws IOException {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) >= 0) {
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }
}
