package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class MultiModalConversationResult {
  private String requestId;
  private MultiModalConversationUsage usage;
  private MultiModalConversationOutput output;

  @SerializedName("status_code")
  private Integer statusCode;

  private String code;
  private String message;

  private MultiModalConversationResult() {}

  public static MultiModalConversationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    MultiModalConversationResult result = new MultiModalConversationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    result.setStatusCode(dashScopeResult.getStatusCode());
    result.setCode(dashScopeResult.getCode());
    result.setMessage(dashScopeResult.getMessage());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), MultiModalConversationUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), MultiModalConversationOutput.class));
    } else {
      log.error("Result no output: {}", dashScopeResult);
    }
    return result;
  }
}
