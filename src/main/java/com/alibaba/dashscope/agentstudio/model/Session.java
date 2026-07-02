// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.agentstudio.model.Configs.ModelConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ToolConfig;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Session extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("title")
  private String title;

  @SerializedName("agent")
  private SessionAgent agent;

  @SerializedName("environment_id")
  private String environmentId;

  @SerializedName("status")
  private String status;

  @SerializedName("stop_reason")
  private StopReason stopReason;

  @SerializedName("resources")
  private List<JsonObject> resources;

  @SerializedName("metadata")
  private Map<String, String> metadata;

  @SerializedName("vault_ids")
  private List<String> vaultIds;

  @SerializedName("stats")
  private Stats stats;

  @SerializedName("usage")
  private Usage usage;

  @SerializedName("archived_at")
  private String archivedAt;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;

  public String getAgentId() {
    if (agent != null) return agent.getId();
    return null;
  }

  public Integer getAgentVersion() {
    if (agent != null) return agent.getVersion();
    return null;
  }

  @Data
  public static class SessionAgent {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("version")
    private Integer version;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("model")
    private ModelConfig model;

    @SerializedName("system")
    private String system;

    @SerializedName("tools")
    private List<ToolConfig> tools;
  }

  @Data
  public static class StopReason {
    @SerializedName("type")
    private String type;

    @SerializedName("event_ids")
    private List<String> eventIds;
  }

  @Data
  public static class Usage {
    @SerializedName("input_tokens")
    private Long inputTokens;

    @SerializedName("output_tokens")
    private Long outputTokens;

    @SerializedName("cache_creation_input_tokens")
    private Long cacheCreationInputTokens;

    @SerializedName("cache_read_input_tokens")
    private Long cacheReadInputTokens;

    @SerializedName("cache_creation")
    private Long cacheCreation;

    @SerializedName("speed")
    private Double speed;
  }

  @Data
  public static class Stats {
    @SerializedName("active_seconds")
    private Double activeSeconds;

    @SerializedName("duration_seconds")
    private Double durationSeconds;
  }
}
