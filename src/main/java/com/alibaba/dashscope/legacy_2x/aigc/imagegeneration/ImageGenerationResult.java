package com.alibaba.dashscope.legacy_2x.aigc.imagegeneration;

import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Deprecated
public class ImageGenerationResult {
  private String requestId;
  private ImageGenerationUsage usage;
  private ImageGenerationOutput output;
  private Map<String, Object> headers;

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
    result.setHeaders(dashScopeResult.getHeaders());
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
      log.error(
          "Result no output: {}, requestId={}",
          dashScopeResult,
          dashScopeResult.getRequestIdOrUnknown());
    }
    return result;
  }
}
