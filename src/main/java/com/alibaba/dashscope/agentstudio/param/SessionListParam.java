// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SessionListParam extends FlattenHalfDuplexParamBase {
  @Default private Integer limit = null;
  @Default private String page = null;
  @Default private String agentId = null;
  @Default private List<String> statuses = null;
  @Default private String createdAtGt = null;
  @Default private String createdAtGte = null;
  @Default private String createdAtLt = null;
  @Default private String createdAtLte = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    AgentStudioConstants.appendParam(sb, "limit", limit);
    AgentStudioConstants.appendParam(sb, "page", page);
    AgentStudioConstants.appendParam(sb, "agent_id", agentId);
    if (statuses != null) {
      for (String s : statuses) {
        AgentStudioConstants.appendParam(sb, "statuses[]", s);
      }
    }
    AgentStudioConstants.appendParam(sb, "created_at[gt]", createdAtGt);
    AgentStudioConstants.appendParam(sb, "created_at[gte]", createdAtGte);
    AgentStudioConstants.appendParam(sb, "created_at[lt]", createdAtLt);
    AgentStudioConstants.appendParam(sb, "created_at[lte]", createdAtLte);
    return sb.toString();
  }
}
