// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.rerank;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class TextReRankResult {
  @SerializedName("request_id")
  private String requestId;

  private TextReRankUsage usage;

  private TextReRankOutput output;

  private TextReRankResult() {}

  public static TextReRankResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    TextReRankResult result = new TextReRankResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), TextReRankUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), TextReRankOutput.class));
    } else {
      log.error("Result no output: {}", dashScopeResult);
    }
    return result;
  }
}
