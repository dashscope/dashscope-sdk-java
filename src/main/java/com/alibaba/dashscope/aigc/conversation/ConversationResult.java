// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.conversation;

import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationUsage;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;

@Data
public final class ConversationResult {
  private String requestId;
  private GenerationUsage usage;
  private GenerationOutput output;

  private ConversationResult() {}

  public static ConversationResult fromDashScopeResult(DashScopeResult dashScopeResult) {
    ConversationResult result = new ConversationResult();
    result.setRequestId(dashScopeResult.getRequestId());
    result.setUsage(
        JsonUtils.fromJsonObject(
            dashScopeResult.getUsage().getAsJsonObject(), GenerationUsage.class));
    result.setOutput(
        JsonUtils.fromJsonObject((JsonObject) dashScopeResult.getOutput(), GenerationOutput.class));
    return result;
  }
}
