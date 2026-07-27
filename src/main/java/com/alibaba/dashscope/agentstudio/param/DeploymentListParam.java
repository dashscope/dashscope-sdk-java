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
public class DeploymentListParam extends FlattenHalfDuplexParamBase {
  @Default private String agentId = null;
  @Default private String keyword = null;
  @Default private String status = null;
  @Default private Boolean includeArchived = null;
  @Default private Integer limit = null;
  @Default private String page = null;
  @Default private String createdAtGte = null;
  @Default private String createdAtLte = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    AgentStudioConstants.appendParam(sb, "agent_id", agentId);
    AgentStudioConstants.appendParam(sb, "keyword", keyword);
    AgentStudioConstants.appendParam(sb, "status", status);
    AgentStudioConstants.appendParam(sb, "include_archived", includeArchived);
    AgentStudioConstants.appendParam(sb, "limit", limit);
    AgentStudioConstants.appendParam(sb, "page", page);
    AgentStudioConstants.appendParam(sb, "created_at[gte]", createdAtGte);
    AgentStudioConstants.appendParam(sb, "created_at[lte]", createdAtLte);
    return sb.toString();
  }
}
