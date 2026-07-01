// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FileListParam extends FlattenHalfDuplexParamBase {
  @Default private Integer limit = null;
  @Default private String page = null;
  @Default private String scopeId = null;
  @Default private String afterId = null;
  @Default private String beforeId = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    AgentStudioConstants.appendParam(sb, "limit", limit);
    AgentStudioConstants.appendParam(sb, "page", page);
    AgentStudioConstants.appendParam(sb, "scope_id", scopeId);
    AgentStudioConstants.appendParam(sb, "after_id", afterId);
    AgentStudioConstants.appendParam(sb, "before_id", beforeId);
    return sb.toString();
  }
}
