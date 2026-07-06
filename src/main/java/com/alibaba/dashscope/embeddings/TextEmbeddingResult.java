// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class TextEmbeddingResult {
  private String requestId;
  private TextEmbeddingOutput output;
  private TextEmbeddingUsage usage;

  @SerializedName("status_code")
  private Integer statusCode;

  private String code;
  private String message;

  private TextEmbeddingResult() {}

  public static TextEmbeddingResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    TextEmbeddingResult res = new TextEmbeddingResult();
    res.output =
        JsonUtils.fromJson((JsonObject) dashScopeResult.getOutput(), TextEmbeddingOutput.class);
    res.usage = JsonUtils.fromJson(dashScopeResult.getUsage(), TextEmbeddingUsage.class);
    res.requestId = dashScopeResult.getRequestId();
    res.statusCode = dashScopeResult.getStatusCode();
    res.code = dashScopeResult.getCode();
    res.message = dashScopeResult.getMessage();
    // Type listType = new TypeToken<ArrayList<TextEmbeddingResultItem>>(){}.getType();
    // res.embeddings = JsonUtils.fromJson(res.output.get("embeddings"), listType);
    return res;
  }
}
