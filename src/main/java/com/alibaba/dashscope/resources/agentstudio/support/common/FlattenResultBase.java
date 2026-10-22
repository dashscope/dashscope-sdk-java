// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.common;

import com.alibaba.dashscope.resources.agentstudio.support.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import lombok.Data;

/**
 * 3.0 copy of the 2.x FlattenResultBase for the agentstudio subtree (2-arg conversion only; the
 * threads-stream overload of the 2.x original is not part of this subtree).
 */
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
