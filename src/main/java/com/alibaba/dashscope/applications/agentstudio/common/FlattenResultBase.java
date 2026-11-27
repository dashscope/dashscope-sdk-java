// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.common;

import com.alibaba.dashscope.applications.agentstudio.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import lombok.Data;

/** Base type for flattened AgentStudio results. */
@Data
public abstract class FlattenResultBase {
  /** The request if. */
  @SerializedName(
      value = "request_id",
      alternate = {"requestId"})
  private String requestId;

  protected FlattenResultBase() {}

  public static <T extends FlattenResultBase> T fromDashScopeResult(
      DashScopeResult dashScopeResult, Type type) {
    T obj = JsonUtils.fromJson((JsonObject) dashScopeResult.getOutput(), type);
    return obj;
  }
}
