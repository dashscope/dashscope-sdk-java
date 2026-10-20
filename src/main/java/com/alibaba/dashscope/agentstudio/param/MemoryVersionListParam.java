// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Query params for {@code GET /memory_stores/{store_id}/memory_versions}.
 *
 * <p>Filters: {@code memoryId}, {@code operation} ({@code created} / {@code modified} / {@code
 * deleted}), {@code createdAtGte} / {@code createdAtLte} (ISO-8601 timestamps; sent on the wire as
 * {@code created_at.gte} / {@code created_at.lte}), and {@code view} ({@code basic} / {@code
 * full}). Versions are returned newest-first.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryVersionListParam extends FlattenHalfDuplexParamBase {
  @Default private String memoryId = null;
  @Default private String operation = null;
  @Default private String createdAtGte = null;
  @Default private String createdAtLte = null;
  @Default private String view = null;
  @Default private Integer limit = null;
  @Default private String page = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    AgentStudioConstants.appendParam(sb, "memory_id", memoryId);
    AgentStudioConstants.appendParam(sb, "operation", operation);
    AgentStudioConstants.appendParam(sb, "created_at.gte", createdAtGte);
    AgentStudioConstants.appendParam(sb, "created_at.lte", createdAtLte);
    AgentStudioConstants.appendParam(sb, "view", view);
    AgentStudioConstants.appendParam(sb, "limit", limit);
    AgentStudioConstants.appendParam(sb, "page", page);
    return sb.toString();
  }
}
