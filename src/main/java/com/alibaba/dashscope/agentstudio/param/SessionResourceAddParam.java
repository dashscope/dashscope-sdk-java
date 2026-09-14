// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SessionResourceAddParam extends FlattenHalfDuplexParamBase {
  @NonNull private String type;
  @NonNull private String fileId;
  @Default private String mountPath = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    body.addProperty("type", type);
    body.addProperty("file_id", fileId);
    if (mountPath != null) {
      body.addProperty("mount_path", mountPath);
    }
    addExtraBody(body);
    return body;
  }
}
