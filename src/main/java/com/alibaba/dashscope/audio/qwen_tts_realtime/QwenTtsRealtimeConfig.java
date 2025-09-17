// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.qwen_tts_realtime;

import static com.alibaba.dashscope.utils.JsonUtils.gson;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/** @author lengjiayi */
@SuperBuilder
@Data
public class QwenTtsRealtimeConfig {
  /** voice to be used in session */
  @NonNull String voice;

  /** response format */
  @Builder.Default
  QwenTtsRealtimeAudioFormat responseFormat = QwenTtsRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT;
  /** mode */
  @Builder.Default String mode = "server_commit";
  /** languageType for tts */
  @Builder.Default String languageType = null;
  /** The extra parameters. */
  @Builder.Default Map<String, Object> parameters = null;

  public JsonObject getConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(QwenTtsRealtimeConstants.VOICE, voice);
    config.put(QwenTtsRealtimeConstants.MODE, mode);
    config.put(QwenTtsRealtimeConstants.RESPONSE_FORMAT, responseFormat.getFormat());
    config.put(QwenTtsRealtimeConstants.SAMPLE_RATE, responseFormat.getSampleRate());
    if (languageType != null) {
      config.put(QwenTtsRealtimeConstants.LANGUAGE_TYPE,languageType);
    }
    if (parameters != null) {
      for (Map.Entry<String, Object> entry : parameters.entrySet()) {
        config.put(entry.getKey(), entry.getValue());
      }
    }

    JsonObject jsonObject = gson.toJsonTree(config).getAsJsonObject();
    return jsonObject;
  }
}
