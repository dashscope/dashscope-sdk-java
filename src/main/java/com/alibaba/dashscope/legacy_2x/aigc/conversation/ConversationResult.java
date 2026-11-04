// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.aigc.conversation;

import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.legacy_2x.aigc.generation.GenerationUsage;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Data;

@Data
@Deprecated
public final class ConversationResult {
  private String requestId;
  private GenerationUsage usage;
  private GenerationOutput output;
  private Map<String, Object> headers;

  private ConversationResult() {}

  public static ConversationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    ConversationResult result = new ConversationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    result.setHeaders(dashScopeResult.getHeaders());
    result.setUsage(
        JsonUtils.fromJsonObject(
            dashScopeResult.getUsage().getAsJsonObject(), GenerationUsage.class));
    result.setOutput(
        JsonUtils.fromJsonObject((JsonObject) dashScopeResult.getOutput(), GenerationOutput.class));
    return result;
  }
}
