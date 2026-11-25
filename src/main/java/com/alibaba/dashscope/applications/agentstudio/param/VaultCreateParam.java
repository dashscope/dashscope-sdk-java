// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.param;

import com.alibaba.dashscope.legacy_2x.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class VaultCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String displayName;
  @Default private Map<String, Object> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("display_name", displayName);
    if (metadata != null && !metadata.isEmpty()) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
