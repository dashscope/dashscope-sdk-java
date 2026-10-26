// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.engineering.cache;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side semantic cache: cut token spend by answering repeat / near-repeat questions from
 * local memory. The query is embedded with {@code client.embeddings()} and compared (cosine
 * similarity) against the questions already cached; at or above a similarity threshold the stored
 * answer is returned with no model call at all, below it the query is sent to {@code client.chat()}
 * and the result is cached. Embedding vectors themselves are memoized by content hash (SHA-256 of
 * the text), so an exact repeat never re-pays the embedding call either.
 *
 * <p>Translation of the Python blueprint example 02_advanced_engineering/client_side_cache.py.
 *
 * <p>The store here is a plain in-memory list; swap in Redis or a vector database for production.
 * Requires DASHSCOPE_API_KEY because cache misses call the real embeddings + chat endpoints.
 */
public final class ClientSideCache {

  private static final String EMBED_MODEL = envOr("DASHSCOPE_EMBED_MODEL", "text-embedding-v4");
  private static final String CHAT_MODEL = envOr("DASHSCOPE_MODEL", "qwen3.7-plus");
  /**
   * Cosine similarity at or above which two questions are treated as the same and the cached answer
   * is reused. Tune to your precision/recall trade-off: higher means fewer false hits, lower means
   * more savings.
   */
  private static final double THRESHOLD = 0.90;

  private ClientSideCache() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

  /** Cosine similarity of two equal-length float vectors. */
  static double cosine(List<Double> a, List<Double> b) {
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

  /** Content-hash key for the embedding memo: SHA-256 hex of the exact text. */
  private static String contentKey(String text) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
    byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
    StringBuilder out = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      out.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    }
    return out.toString();
  }

  /** One cached question: its embedding plus the answer the model gave. */
  private static final class CacheEntry {
    final List<Double> embedding;
    final String question;
    final String answer;

    CacheEntry(List<Double> embedding, String question, String answer) {
      this.embedding = embedding;
      this.question = question;
      this.answer = answer;
    }
  }

  /** In-memory semantic cache backed by DashScope embeddings. */
  static final class SemanticCache {
    private final DashScopeClient client;
    private final double threshold;
    private final List<CacheEntry> entries = new ArrayList<>();
    private final Map<String, List<Double>> embeddingMemo = new HashMap<>();
    int hits;
    int misses;

    SemanticCache(DashScopeClient client, double threshold) {
      this.client = client;
      this.threshold = threshold;
    }

    private List<Double> embed(String text) {
      return embeddingMemo.computeIfAbsent(
          contentKey(text),
          key ->
              client
                  .embeddings()
                  .create(EmbeddingCreateParams.builder().model(EMBED_MODEL).text(text).build())
                  .getEmbeddings()
                  .get(0)
                  .getEmbedding());
    }

    private String chat(String question) {
      return client
          .chat()
          .completions()
          .create(
              ChatCompletionCreateParams.builder()
                  .model(CHAT_MODEL)
                  .messages(Collections.singletonList(Message.user(question)))
                  .build())
          .outputText();
    }

    /** Return the answer for {@code question}; {@code true} when served from cache. */
    Map.Entry<String, Boolean> ask(String question) {
      List<Double> queryVector = embed(question);
      double bestSimilarity = -1.0;
      String bestAnswer = null;
      for (CacheEntry entry : entries) {
        double similarity = cosine(queryVector, entry.embedding);
        if (similarity > bestSimilarity) {
          bestSimilarity = similarity;
          bestAnswer = entry.answer;
        }
      }
      if (bestAnswer != null && bestSimilarity >= threshold) {
        hits++;
        return new AbstractMap.SimpleImmutableEntry<>(bestAnswer, true);
      }
      // Miss: pay for one real completion, then remember it.
      String answer = chat(question);
      entries.add(new CacheEntry(queryVector, question, answer));
      misses++;
      return new AbstractMap.SimpleImmutableEntry<>(answer, false);
    }
  }

  /** Ask a few questions and show which ones hit the cache. */
  public static void main(String[] args) {
    try (DashScopeClient client = ExampleSupport.client()) {
      SemanticCache cache = new SemanticCache(client, THRESHOLD);
      String[] questions = {
        "用一句话介绍杭州",
        "用一句话介绍杭州", // exact repeat -> guaranteed cache hit
        "请简单介绍一下杭州这座城市", // near-duplicate -> likely hit
        "用一句话介绍北京", // different meaning -> cache miss
      };
      for (String question : questions) {
        Map.Entry<String, Boolean> result = cache.ask(question);
        String tag = result.getValue() ? "CACHE HIT " : "CACHE MISS";
        System.out.println("[" + tag + "] Q: " + question);
        System.out.println("           A: " + result.getKey());
      }
      System.out.println(
          "----- hits="
              + cache.hits
              + ", misses="
              + cache.misses
              + ", real chat calls="
              + cache.misses
              + " -----");
    }
  }
}
