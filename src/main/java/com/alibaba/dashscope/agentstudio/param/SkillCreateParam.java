// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SkillCreateParam extends FlattenHalfDuplexParamBase {
  @Default private String fileId = null;
  @Default private String file = null;
  @Default private String mimeType = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (fileId != null) {
      body.addProperty("file_id", fileId);
    }
    addExtraBody(body);
    return body;
  }
}
