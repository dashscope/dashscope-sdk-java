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
public class SecurityListAgentLogsParam extends FlattenHalfDuplexParamBase {
  @Default private Integer currentPage = null;
  @Default private Integer pageSize = null;
  @Default private String riskLevel = null;
  @Default private String status = null;
  @Default private String riskName = null;
  @Default private String appName = null;
  @Default private String assetType = null;
  @Default private String vendor = null;
  @Default private String orderBy = null;
  @Default private String order = null;
  @Default private String lang = null;
  @Default private List<String> statusList = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    AgentStudioConstants.appendParam(sb, "current_page", currentPage);
    AgentStudioConstants.appendParam(sb, "page_size", pageSize);
    AgentStudioConstants.appendParam(sb, "risk_level", riskLevel);
    AgentStudioConstants.appendParam(sb, "status", status);
    AgentStudioConstants.appendParam(sb, "risk_name", riskName);
    AgentStudioConstants.appendParam(sb, "app_name", appName);
    AgentStudioConstants.appendParam(sb, "asset_type", assetType);
    AgentStudioConstants.appendParam(sb, "vendor", vendor);
    AgentStudioConstants.appendParam(sb, "order_by", orderBy);
    AgentStudioConstants.appendParam(sb, "order", order);
    AgentStudioConstants.appendParam(sb, "lang", lang);
    if (statusList != null) {
      for (String s : statusList) {
        AgentStudioConstants.appendParam(sb, "status_list", s);
      }
    }
    return sb.toString();
  }
}
