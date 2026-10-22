// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

/** One reranked document: position in the input list plus relevance score. */
public class RerankResult {
  @JsonProperty("index")
  private int index;

  @JsonProperty("relevance_score")
  private double relevanceScore;

  @JsonProperty("document")
  private RerankDocument document;

  public int getIndex() {
    return index;
  }

  public double getRelevanceScore() {
    return relevanceScore;
  }

  public RerankDocument getDocument() {
    return document;
  }
}
