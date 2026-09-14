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
  @NonNull private String agent;
  @Default private String environmentId = null;
  @Default private String title = null;
  @Default private List<Map<String, Object>> resources = null;
  @Default private List<String> vaultIds = null;
  @Default private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("agent", agent);
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
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
