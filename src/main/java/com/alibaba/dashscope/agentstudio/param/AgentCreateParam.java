// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.model.Configs.McpServerConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.MultiAgentConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.SkillConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ToolConfig;
import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AgentCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String name;
  @NonNull private String model;
  @Default private String description = null;
  @Default private String systemPrompt = null;
  @Default private List<ToolConfig> tools = null;
  @Default private List<McpServerConfig> mcpServers = null;
  @Default private List<SkillConfig> skills = null;
  @Default private MultiAgentConfig multiagent = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("name", name);
    JsonObject modelObj = new JsonObject();
    modelObj.addProperty("id", model);
    body.add("model", modelObj);
    if (description != null) {
      body.addProperty("description", description);
    }
    if (systemPrompt != null) {
      body.addProperty("system", systemPrompt);
    }
    if (tools != null) {
      body.add("tools", JsonUtils.toJsonElement(tools));
    }
    if (mcpServers != null) {
      body.add("mcp_servers", JsonUtils.toJsonElement(mcpServers));
    }
    if (skills != null) {
      body.add("skills", JsonUtils.toJsonElement(skills));
    }
    if (multiagent != null) {
      body.add("multiagent", JsonUtils.toJsonElement(multiagent));
    }
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
