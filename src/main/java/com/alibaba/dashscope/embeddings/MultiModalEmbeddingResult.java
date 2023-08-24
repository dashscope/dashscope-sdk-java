// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class MultiModalEmbeddingResult {
  private String requestId;
  private MultiModalEmbeddingOutput output;
  private MultiModalEmbeddingUsage usage;

  private MultiModalEmbeddingResult() {}

  public static MultiModalEmbeddingResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    MultiModalEmbeddingResult res = new MultiModalEmbeddingResult();
    res.output =
        JsonUtils.fromJson(
            (JsonObject) dashScopeResult.getOutput(), MultiModalEmbeddingOutput.class);
    res.usage =
        JsonUtils.fromJson((JsonObject) dashScopeResult.getUsage(), MultiModalEmbeddingUsage.class);
    res.requestId = dashScopeResult.getRequestId();
    return res;
  }
}
