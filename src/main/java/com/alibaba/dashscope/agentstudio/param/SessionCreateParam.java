// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

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
public class SessionCreateParam extends FlattenHalfDuplexParamBase {
  /** Agent ID string, override Map, or SessionAgent. See {@link SessionAgent}. */
  @NonNull private Object agent;

  @Default private String environmentId = null;
  @Default private String title = null;
  @Default private List<Map<String, Object>> resources = null;
  @Default private List<String> vaultIds = null;
  @Default private Map<String, String> environmentVariables = null;
  @Default private List<Map<String, Object>> mcpConfigs = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.add("agent", SessionAgent.toJsonElement(agent));
    if (environmentId != null) {
      body.addProperty("environment_id", environmentId);
    }
    if (title != null) {
      body.addProperty("title", title);
    }
    if (resources != null && !resources.isEmpty()) {
      body.add("resources", JsonUtils.toJsonElement(resources));
    }
    if (vaultIds != null && !vaultIds.isEmpty()) {
      body.add("vault_ids", JsonUtils.toJsonElement(vaultIds));
    }
    if (environmentVariables != null && !environmentVariables.isEmpty()) {
      body.add("environment_variables", JsonUtils.toJsonElement(environmentVariables));
    }
    if (mcpConfigs != null && !mcpConfigs.isEmpty()) {
      body.add("mcp_configs", JsonUtils.toJsonElement(mcpConfigs));
    }
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
