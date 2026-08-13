// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** Cursor pagination parameters for an endpoint's webhook event audit records. */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class WebhookEventListParam extends FlattenHalfDuplexParamBase {
  @Default private Integer limit = null;
  @Default private String page = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder query = new StringBuilder();
    AgentStudioConstants.appendParam(query, "limit", limit);
    AgentStudioConstants.appendParam(query, "page", page);
    return query.toString();
  }
}
