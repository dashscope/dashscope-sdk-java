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
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SessionUpdateParam extends FlattenHalfDuplexParamBase {
  @Default private String title = null;
  @Default private Map<String, String> metadata = null;
  @Default private List<String> vaultIds = null;
  @Default private Map<String, String> environmentVariables = null;
  /** Agent config patch (override Map or SessionAgent). See {@link SessionAgent}. */
  @Default private Object agent = null;

  @Default private List<Map<String, Object>> mcpConfigs = null;

  @Override
  public boolean shouldSerializeExplicitNulls() {
    // Preserve null sub-fields inside the agent patch on the wire.
    return agent != null;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (title != null) {
      body.addProperty("title", title);
    }
    if (metadata != null) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    if (vaultIds != null) {
      body.add("vault_ids", JsonUtils.toJsonElement(vaultIds));
    }
    if (environmentVariables != null) {
      body.add("environment_variables", JsonUtils.toJsonElement(environmentVariables));
    }
    if (agent != null) {
      body.add("agent", SessionAgent.toJsonElement(agent));
    }
    if (mcpConfigs != null) {
      body.add("mcp_configs", JsonUtils.toJsonElement(mcpConfigs));
    }
    addExtraBody(body);
    return body;
  }
}
