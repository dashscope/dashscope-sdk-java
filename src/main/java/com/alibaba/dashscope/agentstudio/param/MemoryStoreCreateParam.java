// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Request body for {@code POST /memory_stores}.
 *
 * <p>{@code name} must contain 1..255 characters; names may repeat within a workspace, and the
 * session mount slug {@code /mnt/memory/<name>/} is derived from it. {@code description} is passed
 * to the agent — write it for the model, not for humans.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryStoreCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String name;
  @Default private String description = null;
  @Default private Map<String, Object> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("name", name);
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
