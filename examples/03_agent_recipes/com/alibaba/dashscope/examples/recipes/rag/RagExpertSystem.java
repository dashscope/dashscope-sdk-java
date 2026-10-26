// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.recipes.rag;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.embeddings.Embedding;
import com.alibaba.dashscope.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.types.embeddings.EmbeddingResponse;
import com.alibaba.dashscope.types.messages.Message;
import com.alibaba.dashscope.types.rerank.RerankCreateParams;
import com.alibaba.dashscope.types.rerank.RerankResponse;
import com.alibaba.dashscope.types.rerank.RerankResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Agent recipe: a standard RAG loop with Bailian retrieval (embeddings + rerank).
 * Retrieval-augmented generation grounds the model in your own documents so answers cite sources
 * instead of hallucinating. The loop:
 *
 * <ol>
 *   <li>embed a small corpus once ({@code client.embeddings()});
 *   <li>embed the query and take a cosine top-k;
 *   <li>re-score those candidates with {@code client.rerank()} (a cross-encoder) for precision;
 *   <li>stuff the top passages into an "answer only from context" prompt, call {@code
 *       client.chat()}, and print the answer with citations.
 * </ol>
 *
 * <p>Translation of the Python blueprint example 03_agent_recipes/rag_expert_system.py.
 *
 * <p>Requires DASHSCOPE_API_KEY.
 */
public final class RagExpertSystem {

  private static final String EMBED_MODEL = envOr("DASHSCOPE_EMBED_MODEL", "text-embedding-v4");
  private static final String RERANK_MODEL = envOr("DASHSCOPE_RERANK_MODEL", "gte-rerank-v2");
  private static final String CHAT_MODEL = envOr("DASHSCOPE_MODEL", "qwen3.7-plus");
  private static final int TOP_K = 3;

  /** A tiny stand-in corpus; in production load chunks from files or a DB. */
  private static final List<String> CORPUS =
      Arrays.asList(
          "DashScope 是阿里云百炼提供的模型服务 SDK，支持文本生成、向量、重排等能力。",
          "百炼平台提供 qwen 系列大模型，可通过 API Key 调用。",
          "文本向量（embedding）把句子映射为定长浮点向量，用于语义检索。",
          "重排（rerank）用交叉编码器对候选文档按与查询的相关性重新打分。",
          "杭州是浙江省的省会，位于中国东南沿海。");

  private RagExpertSystem() {}

  private static String envOr(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isEmpty() ? fallback : value;
  }

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

  /** Embed a list of texts, returned in input order. */
  private static List<List<Double>> embedTexts(DashScopeClient client, List<String> texts) {
    EmbeddingResponse resp =
        client
            .embeddings()
            .create(EmbeddingCreateParams.builder().model(EMBED_MODEL).texts(texts).build());
    List<Embedding> ordered = new ArrayList<>(resp.getEmbeddings());
    ordered.sort(Comparator.comparingInt(Embedding::getIndex));
    List<List<Double>> vectors = new ArrayList<>(ordered.size());
    for (Embedding item : ordered) {
      vectors.add(item.getEmbedding());
    }
    return vectors;
  }

  /** Indices of the top_k corpus vectors closest to the query. */
  private static List<Integer> retrieve(
      List<Double> queryVector, List<List<Double>> corpusVectors, int topK) {
    List<Double> scores = new ArrayList<>();
    List<Integer> order = new ArrayList<>();
    for (int i = 0; i < corpusVectors.size(); i++) {
      scores.add(cosine(queryVector, corpusVectors.get(i)));
      order.add(i);
    }
    order.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
    return order.subList(0, Math.min(topK, order.size()));
  }

  /** Re-score candidates; return passage text ordered by relevance. */
  private static List<String> rerank(
      DashScopeClient client, String query, List<String> candidates) {
    RerankResponse resp =
        client
            .rerank()
            .create(
                RerankCreateParams.builder()
                    .model(RERANK_MODEL)
                    .query(query)
                    .documents(candidates)
                    .topN(candidates.size())
                    .returnDocuments(true)
                    .build());
    List<String> passages = new ArrayList<>();
    for (RerankResult result : resp.getResults()) {
      if (result.getDocument() != null) {
        passages.add(result.getDocument().getText());
      }
    }
    return passages;
  }

  /** Run the full retrieve -> rerank -> generate loop. */
  static String ragAnswer(DashScopeClient client, String query) {
    List<List<Double>> corpusVectors = embedTexts(client, CORPUS);
    List<Double> queryVector = embedTexts(client, Collections.singletonList(query)).get(0);

    List<Integer> candidateIdx = retrieve(queryVector, corpusVectors, TOP_K);
    List<String> candidates = new ArrayList<>();
    System.out.println("retrieved (cosine top-k):");
    for (int idx : candidateIdx) {
      candidates.add(CORPUS.get(idx));
      System.out.println("  - [" + idx + "] " + CORPUS.get(idx));
    }

    List<String> passages = rerank(client, query, candidates);
    System.out.println("after rerank:");
    for (String passage : passages) {
      System.out.println("  - " + passage);
    }

    StringBuilder context = new StringBuilder();
    for (int n = 0; n < passages.size(); n++) {
      context.append('[').append(n).append("] ").append(passages.get(n)).append('\n');
    }
    String prompt = "仅根据下面的资料回答问题，并标注引用的编号；若资料不足，则回答：资料不足。\n" + "资料：\n" + context + "\n问题：" + query;
    ChatCompletion resp =
        client
            .chat()
            .completions()
            .create(
                ChatCompletionCreateParams.builder()
                    .model(CHAT_MODEL)
                    .messages(Collections.singletonList(Message.user(prompt)))
                    .build());
    System.out.println("chat request_id: " + resp.getRequestId());
    return resp.outputText();
  }

  /** Run the retrieve -> rerank -> generate demo. */
  public static void main(String[] args) {
    try (DashScopeClient client = ExampleSupport.client()) {
      String question = "什么是重排（rerank）？它有什么作用？";
      String answer = ragAnswer(client, question);
      System.out.println("----- answer -----");
      System.out.println(answer);
    }
  }
}
