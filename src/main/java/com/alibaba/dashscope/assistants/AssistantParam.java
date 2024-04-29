package com.alibaba.dashscope.assistants;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.tools.ToolBase;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class AssistantParam extends FlattenHalfDuplexParamBase {
  @NonNull private String model;
  @Default private String name = null;
  @Default private String description = null;
  /** Instructions */
  @SerializedName("instructions")
  @Default
  private String instructions = null;

  @Singular private List<ToolBase> tools;
  /**
   * File Ids
   *
   * <p>(Required)
   */
  @SerializedName("file_ids")
  @Default
  private List<String> fileIds = null;

  /** Metadata */
  @SerializedName("metadata")
  @Default
  private Map<String, String> metadata = null;

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
    if (instructions != null) {
      requestObject.addProperty("instructions", instructions);
    }
    if (tools != null && !tools.isEmpty()) {
      requestObject.add("tools", JsonUtils.toJsonArray(tools));
    }
    if (fileIds != null && !fileIds.isEmpty()) {
      requestObject.add("file_ids", JsonUtils.toJsonArray(fileIds));
    }
    if (metadata != null && !metadata.isEmpty()) {
      requestObject.add("metadata", JsonUtils.toJsonObject(metadata));
    }
    addExtraBody(requestObject);
    return requestObject;
  }
}
