// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DeploymentUpdateParam extends FlattenHalfDuplexParamBase {
  @Default private String name = null;
  @Default private String description = null;
  @Default private DeploymentAgentParam agent = null;
  @Default private String environmentId = null;
  @Default private Boolean clearEnvironment = false;
  @Default private DeploymentScheduleParam schedule = null;
  @Default private Boolean clearSchedule = false;
  @Default private List<JsonObject> initialEvents = null;
  @Default private List<DeploymentResourceParam> resources = null;
  @Default private List<String> vaultIds = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public boolean shouldSerializeExplicitNulls() {
    return Boolean.TRUE.equals(clearEnvironment) || Boolean.TRUE.equals(clearSchedule);
  }

  @Override
  public JsonObject getHttpBody() {
    if (environmentId != null && Boolean.TRUE.equals(clearEnvironment)) {
      throw new IllegalArgumentException("environmentId and clearEnvironment cannot both be set");
    }
    if (schedule != null && Boolean.TRUE.equals(clearSchedule)) {
      throw new IllegalArgumentException("schedule and clearSchedule cannot both be set");
    }
    JsonObject body = new JsonObject();
    if (name != null) {
      body.addProperty("name", name);
    }
    if (description != null) {
      body.addProperty("description", description);
    }
    if (agent != null) {
      body.add("agent", JsonUtils.toJsonElement(agent));
    }
    if (environmentId != null) {
      body.addProperty("environment_id", environmentId);
    } else if (Boolean.TRUE.equals(clearEnvironment)) {
      body.add("environment_id", JsonNull.INSTANCE);
    }
    if (schedule != null) {
      body.add("schedule", JsonUtils.toJsonElement(schedule));
    } else if (Boolean.TRUE.equals(clearSchedule)) {
      body.add("schedule", JsonNull.INSTANCE);
    }
    if (initialEvents != null) {
      JsonArray events = new JsonArray();
      for (JsonObject event : initialEvents) {
        events.add(event);
      }
      body.add("initial_events", events);
    }
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
