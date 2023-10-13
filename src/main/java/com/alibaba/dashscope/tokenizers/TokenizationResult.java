package com.alibaba.dashscope.tokenizers;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class TokenizationResult {
  private String requestId;
  private TokenizationUsage usage;
  private TokenizationOutput output;

  private TokenizationResult() {}

  public static TokenizationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    TokenizationResult result = new TokenizationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.usage =
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), TokenizationUsage.class);
    }
    if (dashScopeResult.getOutput() != null) {
      result.output =
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), TokenizationOutput.class);
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }
    return result;
  }
}
