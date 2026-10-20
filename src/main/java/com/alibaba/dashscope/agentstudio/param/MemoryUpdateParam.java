// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Request body for {@code POST /memory_stores/{store_id}/memories/{memory_id}}.
 *
 * <p>Mutate {@code content}, rename via {@code path}, or both (at least one is required
 * server-side). Passing the current {@code contentSha256} (from the retrieved memory) enables
 * optimistic concurrency — a mismatch returns 409 {@code memory_precondition_failed_error}.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryUpdateParam extends FlattenHalfDuplexParamBase {
  @Default private String path = null;
  @Default private String content = null;
  @Default private String contentSha256 = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (path != null) {
      body.addProperty("path", path);
    }
    if (content != null) {
      body.addProperty("content", content);
    }
    if (contentSha256 != null) {
      body.addProperty("content_sha256", contentSha256);
    }
    addExtraBody(body);
    return body;
  }
}
