package com.alibaba.dashscope.legacy_2x.common;

import com.alibaba.dashscope.legacy_2x.base.FlattenHalfDuplexParamBase;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Deprecated
public class UpdateMetadataParam extends FlattenHalfDuplexParamBase {
  @SerializedName("metadata")
  @Default
  private Map<String, String> metadata = null;

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    if (metadata != null && !metadata.isEmpty()) {
      requestObject.add("metadata", JsonUtils.toJsonObject(metadata));
    }
    return requestObject;
  }
}
