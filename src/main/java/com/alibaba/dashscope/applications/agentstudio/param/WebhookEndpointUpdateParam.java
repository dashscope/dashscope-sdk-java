// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.param;

import com.alibaba.dashscope.applications.agentstudio.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.applications.agentstudio.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/** Parameters for updating a webhook endpoint. */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class WebhookEndpointUpdateParam extends FlattenHalfDuplexParamBase {
  @Default private String description = null;
  @Default private String url = null;
  @Default private List<String> events = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject body = new JsonObject();
    if (description != null) {
      body.addProperty("description", description);
    }
    if (url != null) {
      body.addProperty("url", url);
    }
    if (events != null) {
      body.add("events", JsonUtils.toJsonElement(events));
    }
    addExtraBody(body);
    return body;
  }
}
