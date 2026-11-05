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

  @SerializedName("environment_variables")
  private Map<String, String> environmentVariables;

  @SerializedName("mcp_configs")
  private List<Map<String, Object>> mcpConfigs;

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

    @SerializedName("pending_batch_id")
    private String pendingBatchId;

    @SerializedName("pending_call_ids")
    private List<String> pendingCallIds;
  }

  /**
   * A resource mounted into a session. Fields depend on {@code type}: {@code file} carries {@code
   * fileId}/{@code mountPath}; {@code memory_store} carries {@code memoryStoreId}/{@code
   * access}/{@code instructions}/{@code name}/{@code description} and — when {@code provider} is
   * {@code agentstudio_memory} — {@code entityId}/{@code projectIds}/{@code profileSchema} (Bailian
   * memory mounts carry no mount path); {@code oss} carries {@code region}/{@code bucket}/{@code
   * ossPath}/{@code mountPath}/{@code access}. {@code provider} values are not validated so
   * historical responses stay readable. Memory and OSS mounts are create-only.
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SessionResource extends FlattenResultBase {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("file_id")
    private String fileId;

    @SerializedName("memory_store_id")
    private String memoryStoreId;

    @SerializedName("provider")
    private String provider;

    @SerializedName("entity_id")
    private String entityId;

    @SerializedName("project_ids")
    private List<String> projectIds;

    @SerializedName("profile_schema")
    private String profileSchema;

    @SerializedName("access")
    private String access;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("mount_path")
    private String mountPath;

    @SerializedName("region")
    private String region;

    @SerializedName("bucket")
    private String bucket;

    @SerializedName("oss_path")
    private String ossPath;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static class SessionThread extends FlattenResultBase {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("session_id")
    private String sessionId;

    @SerializedName("parent_thread_id")
    private String parentThreadId;

    @SerializedName("agent")
    private Map<String, Object> agent;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("archived_at")
    private String archivedAt;
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
