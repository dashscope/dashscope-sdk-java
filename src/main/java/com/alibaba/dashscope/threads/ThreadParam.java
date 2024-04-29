package com.alibaba.dashscope.threads;

import com.alibaba.dashscope.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.threads.messages.MessageParamBase;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class ThreadParam extends FlattenHalfDuplexParamBase {
  @Singular private List<MessageParamBase> messages;

  /** Metadata */
  @SerializedName("metadata")
  @Default
  private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    if (messages != null && !messages.isEmpty()) {
      JsonArray messageArray = new JsonArray();
      for (MessageParamBase msg : messages) {
        messageArray.add(msg.getHttpBody());
      }
      requestObject.add("messages", messageArray);
    }
    if (metadata != null && !metadata.isEmpty()) {
      requestObject.add("metadata", JsonUtils.toJsonObject(metadata));
    }
    addExtraBody(requestObject);
    return requestObject;
  }
}
