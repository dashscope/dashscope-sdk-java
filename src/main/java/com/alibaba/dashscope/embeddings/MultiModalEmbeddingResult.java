// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.common.DashScopeResult;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class MultiModalEmbeddingResult {
  private String requestId;
  private JsonObject output;
  private JsonObject usage;

  private MultiModalEmbeddingResult() {}

  public static MultiModalEmbeddingResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    MultiModalEmbeddingResult res = new MultiModalEmbeddingResult();
    res.output = (JsonObject) dashScopeResult.getOutput();
    res.usage = (JsonObject) dashScopeResult.getUsage();
    res.requestId = dashScopeResult.getRequestId();
    return res;
  }
}
