// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.agentstudio.model.Configs.McpServerConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ModelConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.MultiAgentConfig;
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
public class Agent extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

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

  @SerializedName("multiagent")
  private MultiAgentConfig multiagent;

  @SerializedName("version")
  private Integer version;

  @SerializedName("metadata")
  private Map<String, String> metadata;

  @SerializedName("workspace_id")
  private String workspaceId;

  @SerializedName("archived_at")
  private String archivedAt;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;

  public String getSystemPrompt() {
    return system;
  }
}
