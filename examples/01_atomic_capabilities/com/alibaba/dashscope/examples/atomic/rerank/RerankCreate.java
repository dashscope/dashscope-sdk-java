// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.rerank;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.rerank.RerankCreateParams;
import com.alibaba.dashscope.types.rerank.RerankResponse;
import com.alibaba.dashscope.types.rerank.RerankResult;
import java.util.Arrays;

/**
 * Document reranking. Translation of the Python blueprint example
 * 01_atomic_capabilities/rerank/rerank.py.
 */
public final class RerankCreate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "gte-rerank-v2");

    try (DashScopeClient client = ExampleSupport.client()) {
      System.out.println("----- rerank request -----");
      RerankResponse resp =
          client
              .rerank()
              .create(
                  RerankCreateParams.builder()
                      .model(model)
                      .query("哈尔滨在哪？")
                      .documents(Arrays.asList("黑龙江离俄罗斯很近", "哈尔滨是中国黑龙江省的省会，位于中国东北"))
                      .topN(5)
                      .returnDocuments(true)
                      .instruct("Retrieval document that can answer users query.")
                      .build());
      for (RerankResult result : resp.getResults()) {
        String document = result.getDocument() == null ? null : result.getDocument().getText();
        System.out.println(
            "index="
                + result.getIndex()
                + ", score="
                + result.getRelevanceScore()
                + ", document="
                + document);
      }
      System.out.println(
          "usage: "
              + resp.getUsage().getTotalTokens()
              + " tokens, request_id: "
              + resp.getRequestId());
    }
  }
}
