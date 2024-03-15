// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Title Application call result.<br>
 * Description Application call result.<br>
 * Created at 2024-02-23 17:26
 *
 * @since jdk8
 */
@Slf4j
@Data
@ToString
public class ApplicationResult {
  /** Request id of completion call */
  @SerializedName("request_id")
  private String requestId;

  /** Output of app completion call */
  @SerializedName("output")
  private ApplicationOutput output;

  /** Usage of app completion call */
  @SerializedName("usage")
  private ApplicationUsage usage;

  public static ApplicationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    ApplicationResult result = new ApplicationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    if (dashScopeResult.getUsage() != null) {
      result.setUsage(
          JsonUtils.fromJsonObject(
              dashScopeResult.getUsage().getAsJsonObject(), ApplicationUsage.class));
    }
    if (dashScopeResult.getOutput() != null) {
      result.setOutput(
          JsonUtils.fromJsonObject(
              (JsonObject) dashScopeResult.getOutput(), ApplicationOutput.class));
    } else {
      log.error(String.format("Result no output: %s", dashScopeResult));
    }

    return result;
  }
}
