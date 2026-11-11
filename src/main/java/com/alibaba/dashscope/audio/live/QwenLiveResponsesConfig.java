// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class QwenLiveResponsesConfig {
  private String model;
  private String instructions;
  private List<?> tools;
  private Map<String, Object> parameters;

  public JsonObject getConfig() {
    QwenLiveSessionConfig.requireText(model, "responses.model");
    QwenLiveSessionConfig.requireText(instructions, "responses.instructions");
    Map<String, Object> fields =
        QwenLiveSessionConfig.extensionFields(
            parameters, FIELD_MODEL, FIELD_INSTRUCTIONS, FIELD_TOOLS);
    fields.put(FIELD_MODEL, model);
    fields.put(FIELD_INSTRUCTIONS, instructions);
    QwenLiveSessionConfig.putIfSet(fields, FIELD_TOOLS, tools);
    return JsonUtils.toJsonObject(fields).deepCopy();
  }
}
