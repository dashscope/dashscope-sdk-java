// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/** Parameters for creating a webhook endpoint. */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class WebhookEndpointCreateParam extends FlattenHalfDuplexParamBase {
  @Default private String description = null;
  @NonNull private String url;
  @NonNull private List<String> events;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (description != null) {
      body.addProperty("description", description);
    }
    body.addProperty("url", url);
    body.add("events", JsonUtils.toJsonElement(events));
    addExtraBody(body);
    return body;
  }
}
