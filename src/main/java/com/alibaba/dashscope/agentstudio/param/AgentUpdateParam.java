// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.agentstudio.model.Configs.McpServerConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.ModelConfig;
import com.alibaba.dashscope.agentstudio.model.Configs.MultiAgentConfig;
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

  /** The server rejects an update that omits it (AGENT_005); re-send the current name. */
  @Default private String name = null;
  /** Omit it to leave the stored config untouched; sending one replaces it as a whole. */
  @Default private ModelConfig modelConfig = null;

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
    if (version != null) {
      body.addProperty("version", version);
    }
    if (name != null) {
      body.addProperty("name", name);
    }
    if (modelConfig != null) {
      body.add("model", JsonUtils.toJsonElement(modelConfig));
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
    if (multiagent != null) {
      body.add("multiagent", JsonUtils.toJsonElement(multiagent));
    }
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }

  /** The model id, or null when unset; the full config is {@code getModelConfig()}. */
  @Override
  public String getModel() {
    return modelConfig == null ? null : modelConfig.getId();
  }

  /**
   * Alias for calling {@code setModelConfig(ModelConfig)} with {@code ModelConfig.of(modelId)},
   * which clears the parameters stored on the agent.
   */
  public void setModel(String modelId) {
    setModelConfig(ModelConfig.of(modelId));
  }

  @Override
  public void validate() throws InputRequiredException {
    if (version == null) {
      throw new InputRequiredException(
          "version is required for update. Call agents().retrieve(agentId) first to obtain the"
              + " current version (server rejects updates that don't match the latest version).");
    }
    if (modelConfig != null && (modelConfig.getId() == null || modelConfig.getId().isEmpty())) {
      throw new InputRequiredException("model.id is required, e.g. .model(\"qwen3-max\")");
    }
  }

  /** Adds the {@code model(...)} aliases for {@code modelConfig(...)}. */
  public abstract static class AgentUpdateParamBuilder<
          C extends AgentUpdateParam, B extends AgentUpdateParamBuilder<C, B>>
      extends FlattenHalfDuplexParamBase.FlattenHalfDuplexParamBaseBuilder<C, B> {
    /** Alias for calling {@code modelConfig(ModelConfig)} with {@code ModelConfig.of(modelId)}. */
    public B model(String modelId) {
      return modelConfig(ModelConfig.of(modelId));
    }

    public B model(ModelConfig modelConfig) {
      return modelConfig(modelConfig);
    }
  }
}
