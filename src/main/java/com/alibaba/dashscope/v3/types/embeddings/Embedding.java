// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.embeddings;

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
