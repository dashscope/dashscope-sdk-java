// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.imagesynthesis;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ImageSynthesisResult {
  @SerializedName("request_id")
  private String requestId;

  private ImageSynthesisOutput output;
  private ImageSynthesisUsage usage;

  private ImageSynthesisResult() {}

  public static ImageSynthesisResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    ImageSynthesisResult result = new ImageSynthesisResult();
    result.requestId = dashScopeResult.getRequestId();
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), ImageSynthesisUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), ImageSynthesisOutput.class));
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }
    return result;
  }
}
