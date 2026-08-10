package com.alibaba.dashscope.assistants;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * @deprecated The Assistants API (com.alibaba.dashscope.assistants) is deprecated and will be
 *     removed in a future release. Please migrate to the Responses API. See
 *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration details.
 */
@Deprecated
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
