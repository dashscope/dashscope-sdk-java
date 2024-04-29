package com.alibaba.dashscope.assistants;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AssistantFileParam extends FlattenHalfDuplexParamBase {
  @NonNull
  @SerializedName("file_id")
  private String fileId;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty("file_id", fileId);
    addExtraBody(requestObject);
    return requestObject;
  }
}
