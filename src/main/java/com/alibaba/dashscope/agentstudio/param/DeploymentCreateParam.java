// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
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
public class DeploymentCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String name;
  @Default private String description = null;
  @NonNull private DeploymentAgentParam agent;
  @Default private String environmentId = null;
  @Default private DeploymentScheduleParam schedule = null;
  @NonNull private List<JsonObject> initialEvents;
  @Default private List<DeploymentResourceParam> resources = null;
  @Default private List<String> vaultIds = null;
  @Default private Map<String, Object> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("name", name);
    if (description != null) {
      body.addProperty("description", description);
    }
    body.add("agent", JsonUtils.toJsonElement(agent));
    if (environmentId != null) {
      body.addProperty("environment_id", environmentId);
    }
    if (schedule != null) {
      body.add("schedule", JsonUtils.toJsonElement(schedule));
    }
    JsonArray events = new JsonArray();
    for (JsonObject event : initialEvents) {
      events.add(event);
    }
    body.add("initial_events", events);
    if (resources != null) {
      JsonArray resourceItems = new JsonArray();
      for (DeploymentResourceParam resource : resources) {
        resourceItems.add(JsonUtils.toJsonElement(resource));
      }
      body.add("resources", resourceItems);
    }
    if (vaultIds != null) {
      JsonArray vaultItems = new JsonArray();
      for (String vaultId : vaultIds) {
        vaultItems.add(vaultId);
      }
      body.add("vault_ids", vaultItems);
    }
    if (metadata != null) {
      body.add("metadata", JsonUtils.toJsonElement(new LinkedHashMap<>(metadata)));
    }
    addExtraBody(body);
    return body;
  }
}
