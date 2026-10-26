// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.embeddings;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Embeddings response: one vector per input text. */
public class EmbeddingResponse extends ResponseBase {
  @JsonProperty("embeddings")
  private List<Embedding> embeddings;

  @JsonProperty("usage")
  private Usage usage;

  public List<Embedding> getEmbeddings() {
    return embeddings;
  }

  public Usage getUsage() {
    return usage;
  }
}
