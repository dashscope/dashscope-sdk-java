package com.alibaba.dashscope;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.gson.JsonObject;
import io.reactivex.annotations.NonNull;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class TimeoutTestParam extends FlattenHalfDuplexParamBase {
  @NonNull private String model;
  @Default private String name = null;
  @Default private String description = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    if (name != null) {
      requestObject.addProperty("name", name);
    }
    if (description != null) {
      requestObject.addProperty("description", description);
    }
    addExtraBody(requestObject);
    return requestObject;
  }
}
