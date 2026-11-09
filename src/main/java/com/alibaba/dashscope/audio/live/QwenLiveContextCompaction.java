// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class QwenLiveContextCompaction {
  private Boolean enabled;
  private String mode;
  private Map<String, Object> parameters;

  public JsonObject getConfig() {
    QwenLiveSessionConfig.requireText(mode, "context.compaction.mode");
    Map<String, Object> fields =
        QwenLiveSessionConfig.extensionFields(parameters, FIELD_ENABLED, FIELD_MODE);
    QwenLiveSessionConfig.putIfSet(fields, FIELD_ENABLED, enabled);
    fields.put(FIELD_MODE, mode);
    return JsonUtils.toJsonObject(fields).deepCopy();
  }
}
