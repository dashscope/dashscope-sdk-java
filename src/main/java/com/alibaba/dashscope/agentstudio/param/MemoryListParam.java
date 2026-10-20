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
 * Query params for {@code GET /memory_stores/{store_id}/memories}.
 *
 * <p>{@code pathPrefix} scopes the listing (server-validated: must start and end with {@code /} —
 * {@code "/notes/"} matches {@code /notes/a.md} but not {@code /notes_backup/old.md}; a bare {@code
 * "/"} lists the whole store). {@code view} is {@code basic} (metadata only, the default; {@code
 * limit} up to 100) or {@code full} (include {@code content}; {@code limit} is capped at 20 and
 * higher values are rejected with 400 — CMA silently clamps instead). {@code depth} is 0/absent
 * (flat) or 1 (single-level tree) on the wire; larger values are derived client-side from a flat
 * listing (no {@code depth} is sent). With {@code view=full}, the wire still transfers the content
 * of files the derivation cuts — prefer {@code basic} for deep walks.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryListParam extends FlattenHalfDuplexParamBase {
  @Default private String pathPrefix = null;
  @Default private Integer depth = null;
  @Default private String view = null;
  @Default private Integer limit = null;
  @Default private String page = null;

  @Override
  public JsonObject getHttpBody() {
    return new JsonObject();
  }

  public String toQueryString() {
    StringBuilder sb = new StringBuilder();
    AgentStudioConstants.appendParam(sb, "path_prefix", pathPrefix);
    AgentStudioConstants.appendParam(sb, "depth", depth);
    AgentStudioConstants.appendParam(sb, "view", view);
    AgentStudioConstants.appendParam(sb, "limit", limit);
    AgentStudioConstants.appendParam(sb, "page", page);
    return sb.toString();
  }
}
