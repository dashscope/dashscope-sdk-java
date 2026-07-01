// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.model.Configs.McpServerConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.SkillConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ToolConfig;
import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AgentUpdateParam extends FlattenHalfDuplexParamBase {
  /** Required: must equal the server's current (latest) version, or the update fails with 409. */
  @Default private Integer version = null;

  @Default private String name = null;
  @Default private String model = null;
  @Default private String description = null;
  @Default private String systemPrompt = null;
  @Default private List<ToolConfig> tools = null;
  @Default private List<McpServerConfig> mcpServers = null;
  @Default private List<SkillConfig> skills = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (version != null) {
      body.addProperty("version", version);
    }
    if (name != null) {
      body.addProperty("name", name);
    }
    if (model != null) {
      JsonObject modelObj = new JsonObject();
      modelObj.addProperty("id", model);
      body.add("model", modelObj);
    }
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
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (version == null) {
      throw new InputRequiredException(
          "version is required for update. Call agents().retrieve(agentId) first to obtain the"
              + " current version (server rejects updates that don't match the latest version).");
    }
  }
}
