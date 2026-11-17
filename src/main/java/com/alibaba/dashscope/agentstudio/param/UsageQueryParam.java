// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/** Body fields shared by usage summary and trend; dimension-specific validation is server-side. */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UsageQueryParam extends FlattenHalfDuplexParamBase {
  @NonNull private String dimension;
  private String agentId;
  private String agentVersion;
  private String sessionId;
  private TimeRange timeRange;
  private String granularity;
  private Integer pageSize;
  private String pageToken;

  @Data
  @Builder
  public static class TimeRange {
    @NonNull private String startTime;
    @NonNull private String endTime;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("dimension", dimension);
    if (agentId != null) body.addProperty("agent_id", agentId);
    if (agentVersion != null) body.addProperty("agent_version", agentVersion);
    if (sessionId != null) body.addProperty("session_id", sessionId);
    if (granularity != null) body.addProperty("granularity", granularity);
    if (pageSize != null) body.addProperty("page_size", pageSize);
    if (pageToken != null) body.addProperty("page_token", pageToken);
    if (timeRange != null) {
      JsonObject range = new JsonObject();
      range.addProperty("start_time", timeRange.getStartTime());
      range.addProperty("end_time", timeRange.getEndTime());
      body.add("time_range", range);
    }
    addExtraBody(body);
    return body;
  }
}
