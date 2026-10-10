// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision_model;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;

/** The output result of the decision model api. */
@Data
public class DecisionModelResult {
  private String model;

  @SerializedName("request_id")
  private String requestId;

  /** The answers, key is the question id. */
  private Map<String, DecisionAnswer> answers;

  private DecisionModelUsage usage;

  /** The server side latency in milliseconds. */
  @SerializedName("latency_ms")
  private Double latencyMs;

  public static DecisionModelResult fromDashScopeResult(DashScopeResult result) {
    return JsonUtils.fromJsonObject((JsonObject) result.getOutput(), DecisionModelResult.class);
  }
}
