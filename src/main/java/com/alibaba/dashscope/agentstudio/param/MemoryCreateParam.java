// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Request body for {@code POST /memory_stores/{store_id}/memories}.
 *
 * <p>{@code path} rules (enforced server-side, case-sensitive): POSIX style with a leading {@code
 * /} and no trailing {@code /}; 2..1024 UTF-8 bytes; NFC-normalized; every segment non-empty and
 * not {@code .} or {@code ..}; no control / format / line- or paragraph-separator code points.
 * {@code content} is UTF-8 text, at most 102400 bytes. A memory already existing at {@code path}
 * returns 409 {@code memory_path_conflict_error}.
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MemoryCreateParam extends FlattenHalfDuplexParamBase {
  @NonNull private String path;
  @NonNull private String content;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("path", path);
    body.addProperty("content", content);
    addExtraBody(body);
    return body;
  }
}
