// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.aigc.codegeneration;

import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.alibaba.dashscope.legacy_2x.utils.StringUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Deprecated
public final class CodeGenerationResult {
  private String requestId;
  private CodeGenerationUsage usage;
  private CodeGenerationOutput output;
  private Map<String, Object> headers;

  private CodeGenerationResult() {}

  public static CodeGenerationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    CodeGenerationResult result = new CodeGenerationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    result.setHeaders(dashScopeResult.getHeaders());
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
      log.error(
          StringUtils.format(
              "Result no output: %s, requestId=%s",
              dashScopeResult, dashScopeResult.getRequestIdOrUnknown()));
    }
    return result;
  }
}
