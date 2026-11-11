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
public class QwenLiveContext {
  private QwenLiveContextCompaction compaction;
  private Map<String, Object> parameters;

  public JsonObject getConfig() {
    if (compaction == null) {
      throw new IllegalArgumentException("context.compaction is required.");
    }
    Map<String, Object> fields =
        QwenLiveSessionConfig.extensionFields(parameters, FIELD_COMPACTION);
    fields.put(FIELD_COMPACTION, compaction.getConfig());
    return JsonUtils.toJsonObject(fields).deepCopy();
  }
}
