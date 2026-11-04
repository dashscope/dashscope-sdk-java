// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.multimodal.tingwu;

import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.utils.ApiKeywords;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode()
@Deprecated
public class TingWuRealtimeResult {
  @SerializedName(ApiKeywords.TASKID)
  private String taskId;

  private String action;

  private JsonObject output;

  private JsonObject usage;

  public static TingWuRealtimeResult fromDashScopeResult(DashScopeResult dashScopeResult)
      throws ApiException {
    TingWuRealtimeResult result = new TingWuRealtimeResult();
    result.setTaskId(dashScopeResult.getRequestId());

    JsonObject jsonDashScopeResult = (JsonObject) dashScopeResult.getOutput();
    result.output = jsonDashScopeResult;

    if (dashScopeResult.getUsage() != null) {
      result.usage = dashScopeResult.getUsage().getAsJsonObject();
    }

    if (jsonDashScopeResult.has("action")) {
      result.action = jsonDashScopeResult.get("action").getAsString();
    }
    return result;
  }
}
