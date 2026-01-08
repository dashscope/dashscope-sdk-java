package com.alibaba.dashscope.aigc.imagegeneration;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ImageGenerationResult {
  private String requestId;
  private ImageGenerationUsage usage;
  private ImageGenerationOutput output;

  @SerializedName("status_code")
  private Integer statusCode;

  private String code;
  private String message;

  private ImageGenerationResult() {}

  public static ImageGenerationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    ImageGenerationResult result = new ImageGenerationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    result.setStatusCode(dashScopeResult.getStatusCode());
    result.setCode(dashScopeResult.getCode());
    result.setMessage(dashScopeResult.getMessage());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), ImageGenerationUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), ImageGenerationOutput.class));
    } else {
      log.error("Result no output: {}", dashScopeResult);
    }
    return result;
  }
}
