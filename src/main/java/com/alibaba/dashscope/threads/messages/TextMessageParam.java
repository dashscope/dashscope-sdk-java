package com.alibaba.dashscope.threads.messages;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class TextMessageParam extends MessageParamBase {
  /** The role, can be `user` and `system` and `assistant` and 'tool'. */
  @NonNull String role;

  /** The conversation content. */
  @NonNull String content;

  @SerializedName("file_ids")
  List<String> fileIds;
  /** Metadata */
  @SerializedName("metadata")
  @Default
  private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty("role", role);
    requestObject.addProperty("content", content);
    if (fileIds != null && !fileIds.isEmpty()) {
      requestObject.add("file_ids", JsonUtils.toJsonArray(fileIds));
    }
    if (metadata != null && !metadata.isEmpty()) {
      requestObject.add("metadata", JsonUtils.toJsonObject(metadata));
    }
    addExtraBody(requestObject);
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (role.equals("")
        || content.equals("")
        || !(role.equals("user") || role.equals("assistant"))) {
      throw new InputRequiredException("role mast be set and mast one of[user|assistant]");
    }
  }
}
