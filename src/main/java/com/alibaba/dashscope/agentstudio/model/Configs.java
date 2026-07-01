// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

public final class Configs {
  private Configs() {}

  @Data
  public static class ModelConfig {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;
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
    }

    @Data
    public static class PerToolConfig {
      @SerializedName("name")
      private String name;

      @SerializedName("enabled")
      private Boolean enabled;
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
}
