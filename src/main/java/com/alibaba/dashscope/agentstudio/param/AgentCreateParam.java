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
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AgentCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String name;

  /** Required: the model and its inference parameters. */
  @NonNull private ModelConfig modelConfig;

  @Default private String description = null;
  @Default private String systemPrompt = null;
  @Default private List<ToolConfig> tools = null;
  @Default private List<McpServerConfig> mcpServers = null;
  @Default private List<SkillConfig> skills = null;
  @Default private MultiAgentConfig multiagent = null;
  @Default private Map<String, String> metadata = null;

  /** The model id; the full config is {@code getModelConfig()}. */
  @Override
  public String getModel() {
    return modelConfig.getId();
  }

  /** Alias for calling {@code setModelConfig(ModelConfig)} with {@code ModelConfig.of(modelId)}. */
  public void setModel(String modelId) {
    setModelConfig(ModelConfig.of(modelId));
  }

  @Override
  public void validate() throws InputRequiredException {
    if (modelConfig == null || modelConfig.getId() == null || modelConfig.getId().isEmpty()) {
      throw new InputRequiredException("model.id is required, e.g. .model(\"qwen3-max\")");
    }
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("name", name);
    body.add("model", JsonUtils.toJsonElement(modelConfig));
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

  /** Adds the {@code model(...)} aliases for {@code modelConfig(...)}. */
  public abstract static class AgentCreateParamBuilder<
          C extends AgentCreateParam, B extends AgentCreateParamBuilder<C, B>>
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
