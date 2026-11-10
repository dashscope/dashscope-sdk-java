// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.embeddings;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** One embedding vector; {@code type} says which modality produced it. */
public class Embedding {
  // The text endpoint reports the position as "text_index"; every other endpoint calls it
  // "index". One public name, both wire spellings.
  @JsonProperty("index")
  @JsonAlias("text_index")
  private int index;

  @JsonProperty("type")
  private String type;

  @JsonProperty("embedding")
  private List<Double> embedding;

  // Present only when the request set output_type to "sparse" or "dense&sparse"; the dense
  // embedding above is then empty or accompanied by these sparse terms.
  @JsonProperty("sparse_embedding")
  private List<SparseEmbedding> sparseEmbedding;

  public int getIndex() {
    return index;
  }

  public String getType() {
    return type;
  }

  public List<Double> getEmbedding() {
    return embedding;
  }

  public List<SparseEmbedding> getSparseEmbedding() {
    return sparseEmbedding;
  }
}
