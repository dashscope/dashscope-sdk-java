// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.multimodal;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Multimodal embedding response: one vector per input content. */
public class MultimodalEmbeddingResponse extends ResponseBase {
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

  public static class Embedding {
    @JsonProperty("index")
    private int index;

    @JsonProperty("type")
    private String type;

    @JsonProperty("embedding")
    private List<Double> embedding;

    public int getIndex() {
      return index;
    }

    public String getType() {
      return type;
    }

    public List<Double> getEmbedding() {
      return embedding;
    }
  }
}
