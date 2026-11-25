// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.resource;

import com.alibaba.dashscope.applications.agentstudio.message.ContentBlock;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.ToNumberPolicy;
import java.lang.reflect.Type;

/**
 * Response parsing for AgentStudio resources.
 *
 * <p>AgentStudio models embed polymorphic {@link ContentBlock} values, which require {@link
 * ContentBlock.Deserializer} on the parsing Gson. The shared legacy {@code JsonUtils} Gson cannot
 * register that adapter (legacy code must not depend on 3.0 packages), so AgentStudio responses are
 * parsed here instead of through {@code FlattenResultBase.fromDashScopeResult}. Number and escaping
 * policies mirror {@code JsonUtils} so field-level parse behavior stays the same.
 */
public final class AgentStudioJson {
  public static final Gson GSON =
      new GsonBuilder()
          .registerTypeAdapter(ContentBlock.class, new ContentBlock.Deserializer())
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .disableHtmlEscaping()
          .create();

  private AgentStudioJson() {}

  /**
   * Mirrors {@code FlattenResultBase.fromDashScopeResult}, but parses with {@link #GSON} so
   * responses carrying content blocks deserialize to their concrete subtypes.
   */
  public static <T> T fromDashScopeResult(DashScopeResult dashScopeResult, Type type) {
    return GSON.fromJson((JsonObject) dashScopeResult.getOutput(), type);
  }
}
