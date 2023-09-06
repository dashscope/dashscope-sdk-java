// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class BatchTextEmbeddingResult {
  @SerializedName("request_id")
  private String requestId;

  private BatchTextEmbeddingOutput output;
  private BatchTextEmbeddingUsage usage;

  private BatchTextEmbeddingResult() {}

  public static BatchTextEmbeddingResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    BatchTextEmbeddingResult res = new BatchTextEmbeddingResult();
    res.output =
        JsonUtils.fromJson(
            (JsonObject) dashScopeResult.getOutput(), BatchTextEmbeddingOutput.class);
    res.usage = JsonUtils.fromJson(dashScopeResult.getUsage(), BatchTextEmbeddingUsage.class);
    res.requestId = dashScopeResult.getRequestId();
    return res;
  }
}
