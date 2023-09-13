// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.nlp.understanding;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public final class UnderstandingResult {
  private String requestId;
  private UnderstandingUsage usage;
  private UnderstandingOutput output;

  private UnderstandingResult() {}

  public static UnderstandingResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    UnderstandingResult result = new UnderstandingResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), UnderstandingUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), UnderstandingOutput.class));
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }
    return result;
  }
}
