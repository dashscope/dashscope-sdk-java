// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.decision;

import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
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

  /** The answers, key is the caller-defined question id from the request. */
  private Map<String, DecisionAnswer> answers;

  /** Metering info. The doc currently defines only {@code input_tokens}. */
  private DecisionModelUsage usage;

  /** The server side latency in milliseconds. */
  @SerializedName("latency_ms")
  private Double latencyMs;

  public static DecisionModelResult fromDashScopeResult(DashScopeResult result) {
    if (result == null) {
      throw new IllegalArgumentException("DashScopeResult must not be null");
    }
    Object output = result.getOutput();
    if (output == null) {
      throw new IllegalStateException(
          "Decision model response has no output payload; request_id="
              + (result.getRequestId() == null ? "<none>" : result.getRequestId()));
    }
    if (!(output instanceof JsonObject)) {
      throw new IllegalStateException(
          "Decision model response output is not a JSON object: " + output.getClass().getName());
    }
    JsonElement element = (JsonObject) output;
    DecisionModelResult parsed = JsonUtils.fromJsonObject(element, DecisionModelResult.class);
    if (parsed != null && parsed.getRequestId() == null && result.getRequestId() != null) {
      parsed.setRequestId(result.getRequestId());
    }
    return parsed;
  }
}
