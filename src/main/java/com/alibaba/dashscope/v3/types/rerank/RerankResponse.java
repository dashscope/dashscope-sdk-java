// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.rerank;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Rerank response: results ordered by descending relevance. */
public class RerankResponse extends ResponseBase {
  @JsonProperty("results")
  private List<RerankResult> results;

  @JsonProperty("usage")
  private Usage usage;

  public List<RerankResult> getResults() {
    return results;
  }

  public Usage getUsage() {
    return usage;
  }
}
