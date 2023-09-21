// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.codegeneration;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public final class CodeGenerationResult {
  private String requestId;
  private CodeGenerationUsage usage;
  private CodeGenerationOutput output;

  private CodeGenerationResult() {}

  public static CodeGenerationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    CodeGenerationResult result = new CodeGenerationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), CodeGenerationUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), CodeGenerationOutput.class));
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }
    return result;
  }
}
