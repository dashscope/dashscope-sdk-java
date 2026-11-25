// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.audio.live;

import static com.alibaba.dashscope.legacy_2x.audio.live.QwenLiveConstants.FIELD_TYPE;

import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@Deprecated
public class QwenLiveTransport {
  private String type;
  private Map<String, Object> parameters;

  public JsonObject getConfig() {
    QwenLiveSessionConfig.requireText(type, "transport.type");
    Map<String, Object> fields = QwenLiveSessionConfig.extensionFields(parameters, FIELD_TYPE);
    fields.put(FIELD_TYPE, type);
    return JsonUtils.toJsonObject(fields).deepCopy();
  }
}
