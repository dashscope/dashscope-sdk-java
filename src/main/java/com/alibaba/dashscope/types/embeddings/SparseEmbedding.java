// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.embeddings;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One sparse-vector term returned when {@code output_type} is {@code sparse} or {@code
 * dense&sparse}. {@code index} is the vocabulary position, {@code value} its weight, and {@code
 * token} the decoded text (may be absent for some models).
 */
public class SparseEmbedding {
  @JsonProperty("index")
  private Integer index;

  @JsonProperty("value")
  private Double value;

  @JsonProperty("token")
  private String token;

  public Integer getIndex() {
    return index;
  }

  public Double getValue() {
    return value;
  }

  public String getToken() {
    return token;
  }
}
