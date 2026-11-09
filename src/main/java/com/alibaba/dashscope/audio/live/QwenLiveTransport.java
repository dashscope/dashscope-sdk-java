// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.FIELD_TYPE;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
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
