// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SessionEventSendParam extends FlattenHalfDuplexParamBase {
  @NonNull private List<JsonObject> input;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    JsonArray arr = new JsonArray();
    for (JsonObject event : input) {
      arr.add(event);
    }
    body.add("input", arr);
    addExtraBody(body);
    return body;
  }
}
