// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.embeddings;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.embeddings.Embedding;
import com.alibaba.dashscope.v3.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.v3.types.embeddings.EmbeddingResponse;
import java.util.Arrays;
import java.util.List;

/**
 * Text embeddings: one batch create call. Translation of the Python blueprint example
 * 01_atomic_capabilities/embeddings/embeddings.py.
 */
public final class EmbeddingsCreate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "text-embedding-v4");
    List<String> texts = Arrays.asList("杭州是浙江省的省会", "上海是中国的直辖市");

    try (DashScopeClient client = ExampleSupport.client()) {
      System.out.println("----- embeddings request -----");
      EmbeddingResponse resp =
          client
              .embeddings()
              .create(
                  EmbeddingCreateParams.builder()
                      .model(model)
                      .texts(texts)
                      .dimension(1024)
                      .build());
      for (Embedding item : resp.getEmbeddings()) {
        System.out.println(
            "index=" + item.getIndex() + ", embedding dim=" + item.getEmbedding().size());
      }
      System.out.println("usage: " + resp.getUsage().getTotalTokens() + " tokens");

      // Batch embedding over a file (one text per line) is available via the async-task facade:
      //   Job job =
      //       client.embeddings().jobs().create(
      //           com.alibaba.dashscope.v3.types.embeddings.BatchEmbeddingCreateParams.builder()
      //               .model(model)
      //               .fileUrl("https://.../texts.txt")
      //               .build());
      //   Job result =
      //       client.embeddings().jobs().wait(job.getTaskId(),
      //           com.alibaba.dashscope.RequestOptions.NONE);
    }
  }
}
