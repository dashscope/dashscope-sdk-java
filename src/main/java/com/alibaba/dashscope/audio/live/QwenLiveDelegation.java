// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class QwenLiveDelegation {
  private QwenLiveDelegationType type;
  private QwenLiveResponsesConfig responses;

  public JsonObject getConfig() {
    if (type == null) {
      throw new IllegalArgumentException("delegation.type is required.");
    }
    Map<String, Object> fields = new LinkedHashMap<>();
    fields.put(FIELD_TYPE, type);
    if (type == QwenLiveDelegationType.RESPONSES) {
      if (responses == null) {
        throw new IllegalArgumentException("delegation.responses is required in responses mode.");
      }
      fields.put(FIELD_RESPONSES, responses.getConfig());
    } else if (responses != null) {
      throw new IllegalArgumentException("only responses delegation may carry responses.");
    }
    return JsonUtils.toJsonObject(fields).deepCopy();
  }
}
