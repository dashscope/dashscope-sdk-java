// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.audio.live.QwenLiveConstants.*;

import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class QwenLiveSessionConfig {
  private String model;
  private String instructions;
  private String voice;
  private String inputAudioFormat;
  private String outputAudioFormat;
  private List<String> modalities;
  private List<?> tools;
  private QwenLiveDelegation delegation;
  private QwenLiveTransport transport;
  private QwenLiveContext context;
  private Map<String, Object> parameters;

  public JsonObject getConfig() {
    requireText(model, FIELD_MODEL);
    if (delegation == null) {
      throw new IllegalArgumentException("session.delegation is required.");
    }
    Map<String, Object> fields =
        extensionFields(
            parameters,
            FIELD_MODEL,
            FIELD_INSTRUCTIONS,
            FIELD_VOICE,
            FIELD_INPUT_AUDIO_FORMAT,
            FIELD_OUTPUT_AUDIO_FORMAT,
            FIELD_MODALITIES,
            FIELD_TOOLS,
            FIELD_DELEGATION,
            FIELD_TRANSPORT,
            FIELD_CONTEXT);
    fields.put(FIELD_MODEL, model);
    putIfSet(fields, FIELD_INSTRUCTIONS, instructions);
    putIfSet(fields, FIELD_VOICE, voice);
    putIfSet(fields, FIELD_INPUT_AUDIO_FORMAT, inputAudioFormat);
    putIfSet(fields, FIELD_OUTPUT_AUDIO_FORMAT, outputAudioFormat);
    putIfSet(fields, FIELD_MODALITIES, modalities);
    putIfSet(fields, FIELD_TOOLS, tools);
    if (context != null) {
      fields.put(FIELD_CONTEXT, context.getConfig());
    }
    fields.put(FIELD_DELEGATION, delegation.getConfig());
    if (transport != null) {
      fields.put(FIELD_TRANSPORT, transport.getConfig());
    }
    return JsonUtils.toJsonObject(fields).deepCopy();
  }

  static Map<String, Object> extensionFields(Map<String, Object> parameters, String... reserved) {
    Map<String, Object> fields = new LinkedHashMap<>();
    if (parameters != null) {
      fields.putAll(parameters);
    }
    for (String key : reserved) {
      fields.remove(key);
    }
    return fields;
  }

  static void putIfSet(Map<String, Object> fields, String key, Object value) {
    if (value != null) {
      fields.put(key, value);
    }
  }

  static void requireText(String value, String field) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank.");
    }
  }
}
