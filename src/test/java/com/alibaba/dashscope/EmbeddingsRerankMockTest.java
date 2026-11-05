// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.types.embeddings.EmbeddingResponse;
import com.alibaba.dashscope.types.rerank.RerankCreateParams;
import com.alibaba.dashscope.types.rerank.RerankResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class EmbeddingsRerankMockTest {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private MockWebServer server;
  private DashScopeClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    client =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build();
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.shutdown();
  }

  private static MockResponse okJson(String json) {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody(json);
  }

  @Test
  void embeddingsHappyPathWithTextIndexAlias() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"embeddings\":[{\"text_index\":0,\"embedding\":[0.1,0.2]},"
                + "{\"text_index\":1,\"embedding\":[0.3]}]},\"usage\":{\"input_tokens\":4,"
                + "\"total_tokens\":4},\"request_id\":\"e1\"}"));

    EmbeddingResponse response =
        client
            .embeddings()
            .create(
                EmbeddingCreateParams.builder()
                    .model("text-embedding-v3")
                    .texts(Arrays.asList("你好", "世界"))
                    .textType("query")
                    .dimension(1024)
                    .build());

    assertEquals(2, response.getEmbeddings().size());
    assertEquals(0, response.getEmbeddings().get(0).getIndex());
    assertEquals(Arrays.asList(0.1, 0.2), response.getEmbeddings().get(0).getEmbedding());
    assertEquals(1, response.getEmbeddings().get(1).getIndex());
    assertEquals(4, response.getUsage().getInputTokens());
    assertEquals("e1", response.getRequestId());
    assertFalse(response.isError());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/embeddings/text-embedding/text-embedding", request.getPath());
    assertEquals(
        "java-sdk/" + SdkInfo.VERSION + "/embeddings3",
        request.getHeader("x-dashscope-sdk-client"));
    JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
    assertEquals("text-embedding-v3", body.get("model").asText());
    assertEquals(2, body.get("input").get("texts").size());
    assertEquals("query", body.get("parameters").get("text_type").asText());
    assertEquals(1024, body.get("parameters").get("dimension").asInt());
  }

  @Test
  void embeddingsOnePeaceOutputNormalizedToItemZero() {
    server.enqueue(okJson("{\"output\":{\"embedding\":[0.9,0.8]},\"request_id\":\"e2\"}"));

    EmbeddingResponse response =
        client
            .embeddings()
            .create(
                EmbeddingCreateParams.builder()
                    .model("multimodal-embedding-one-peace-v1")
                    .text("你好")
                    .build());

    assertEquals(1, response.getEmbeddings().size());
    assertEquals(0, response.getEmbeddings().get(0).getIndex());
    assertEquals(Arrays.asList(0.9, 0.8), response.getEmbeddings().get(0).getEmbedding());
  }

  @Test
  void rerankHappyPath() throws Exception {
    server.enqueue(
        okJson(
            "{\"output\":{\"results\":[{\"index\":1,\"relevance_score\":0.9,"
                + "\"document\":{\"text\":\"doc-b\"}},{\"index\":0,\"relevance_score\":0.1,"
                + "\"document\":{\"text\":\"doc-a\"}}]},\"usage\":{\"total_tokens\":10},"
                + "\"request_id\":\"rr1\"}"));

    RerankResponse response =
        client
            .rerank()
            .create(
                RerankCreateParams.builder()
                    .model("gte-rerank-v2")
                    .query("什么是文本向量")
                    .documents(Arrays.asList("doc-a", "doc-b"))
                    .topN(2)
                    .returnDocuments(true)
                    .build());

    assertEquals(2, response.getResults().size());
    assertEquals(1, response.getResults().get(0).getIndex());
    assertEquals(0.9, response.getResults().get(0).getRelevanceScore(), 1e-9);
    assertEquals("doc-b", response.getResults().get(0).getDocument().getText());
    assertEquals(10, response.getUsage().getTotalTokens());
    assertEquals("rr1", response.getRequestId());

    RecordedRequest request = server.takeRequest(5, TimeUnit.SECONDS);
    assertEquals("/services/rerank/text-rerank/text-rerank", request.getPath());
    assertEquals(
        "java-sdk/" + SdkInfo.VERSION + "/rerank3", request.getHeader("x-dashscope-sdk-client"));
    JsonNode body = MAPPER.readTree(request.getBody().readUtf8());
    assertEquals("gte-rerank-v2", body.get("model").asText());
    assertEquals("什么是文本向量", body.get("input").get("query").asText());
    assertEquals(2, body.get("input").get("documents").size());
    assertEquals(2, body.get("parameters").get("top_n").asInt());
    assertTrue(body.get("parameters").get("return_documents").asBoolean());
  }
}
