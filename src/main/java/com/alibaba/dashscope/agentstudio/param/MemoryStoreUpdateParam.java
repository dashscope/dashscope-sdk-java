// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Request body for {@code POST /memory_stores/{store_id}}.
 *
 * <p>{@code metadata} is replaced wholesale (unlike CMA's merge-patch): keys not included here are
 * dropped; a key set to {@code null} deletes it. To change one key, re-send the whole mapping.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryStoreUpdateParam extends FlattenHalfDuplexParamBase {
  @Default private String name = null;
  @Default private String description = null;
  @Default private Map<String, Object> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (name != null) {
      body.addProperty("name", name);
    }
    if (description != null) {
      body.addProperty("description", description);
    }
    if (metadata != null) {
      body.add("metadata", JsonUtils.toJsonElement(metadata));
    }
    addExtraBody(body);
    return body;
  }
}
