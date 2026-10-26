// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class Configs {
  private Configs() {}

  /**
   * The {@code model} object of an agent: the model id plus its inference parameters. Unset
   * parameters keep their server default; on update this object replaces the whole config.
   */
  @Data
  @Builder(toBuilder = true)
  @NoArgsConstructor
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class ModelConfig {
    /**
     * The id-only config. A null id yields null rather than an id-less config, so an unset model
     * stays unset in the request body.
     */
    public static ModelConfig of(String id) {
      return id == null ? null : builder().id(id).build();
    }

    @SerializedName("id")
    private String id;

    /** Console-only: the public API neither accepts nor returns it. */
    @SerializedName("name")
    private String name;

    @SerializedName("temperature")
    private Double temperature;

    @SerializedName("max_tokens")
    private Integer maxTokens;

    @SerializedName("enable_thinking")
    private Boolean enableThinking;

    @SerializedName("thinking_budget")
    private Integer thinkingBudget;
  }

  @Data
  public static class ToolConfig {
    @SerializedName("type")
    private String type;

    @SerializedName("mcp_server_name")
    private String mcpServerName;

    @SerializedName("default_config")
    private DefaultConfig defaultConfig;

    @SerializedName("configs")
    private List<PerToolConfig> configs;

    @Data
    public static class DefaultConfig {
      @SerializedName("enabled")
      private Boolean enabled;

      @SerializedName("permission_policy")
      private PermissionPolicy permissionPolicy;
    }

    @Data
    public static class PerToolConfig {
      @SerializedName("name")
      private String name;

      @SerializedName("enabled")
      private Boolean enabled;

      @SerializedName("permission_policy")
      private PermissionPolicy permissionPolicy;
    }
  }

  @Data
  public static class SkillConfig {
    @SerializedName("type")
    private String type;

    @SerializedName("skill_id")
    private String skillId;

    @SerializedName("version")
    private String version;
  }

  @Data
  public static class McpServerConfig {
    @SerializedName("type")
    private String type;

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;
  }

  /**
   * The {@code multiagent} field on an agent: {@code type} is currently always {@code
   * "coordinator"}, {@code agents} is the roster of 1-20 entries (an empty list clears it).
   */
  @Data
  public static class MultiAgentConfig {
    @SerializedName("type")
    private String type;

    @SerializedName("agents")
    private List<RosterEntry> agents;

    /**
     * One roster entry: {@code type} is {@code "agent"} (reference another agent by {@code id} plus
     * optional {@code version}) or {@code "self"} (a copy of the coordinator; at most one).
     */
    @Data
    public static class RosterEntry {
      @SerializedName("type")
      private String type;

      @SerializedName("id")
      private String id;

      @SerializedName("version")
      private Integer version;

      @SerializedName("name")
      private String name;

      @SerializedName("description")
      private String description;
    }
  }

  @Data
  public static class PermissionPolicy {
    @SerializedName("type")
    private String type;
  }
}
