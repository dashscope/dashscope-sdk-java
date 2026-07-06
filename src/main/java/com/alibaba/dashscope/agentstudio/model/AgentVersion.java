// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.agentstudio.model.Configs.McpServerConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ModelConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.SkillConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ToolConfig;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentVersion extends FlattenResultBase {
  @SerializedName("agent_id")
  private String agentId;

  @SerializedName("version")
  private Integer version;

  @SerializedName("config")
  private AgentVersionConfig config;

  @SerializedName("created_at")
  private String createdAt;

  @Data
  public static class AgentVersionConfig {
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

    @SerializedName("skills")
    private List<SkillConfig> skills;

    @SerializedName("mcp_servers")
    private List<McpServerConfig> mcpServers;

    @SerializedName("metadata")
    private Map<String, String> metadata;
  }
}
