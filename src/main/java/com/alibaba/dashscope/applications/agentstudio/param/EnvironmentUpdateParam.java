// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.param;

import com.alibaba.dashscope.applications.agentstudio.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.applications.agentstudio.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class EnvironmentUpdateParam extends FlattenHalfDuplexParamBase {
  @Default private String name = null;
  @Default private String description = null;
  @Default private Map<String, Object> config = null;
  @Default private String scope = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (name != null) {
      body.addProperty("name", name);
    }
    if (description != null) {
      body.addProperty("description", description);
    }
    if (config != null) {
      body.add("config", JsonUtils.toJsonElement(config));
    }
    if (scope != null) {
      body.addProperty("scope", scope);
    }
    if (metadata != null) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
