// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.videosynthesis;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class VideoSynthesisResult {
  @SerializedName("request_id")
  private String requestId;

  private VideoSynthesisOutput output;
  private VideoSynthesisUsage usage;

  private VideoSynthesisResult() {}

  public static VideoSynthesisResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    VideoSynthesisResult result = new VideoSynthesisResult();
    result.requestId = dashScopeResult.getRequestId();
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), VideoSynthesisUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      VideoSynthesisOutput outputTemp =
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), VideoSynthesisOutput.class);
      result.setOutput(outputTemp);
    } else {
      log.error("Result no output: {}", dashScopeResult);
    }
    return result;
  }
}
