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

  /** sampleRate for tts , range [8000,16000,22050,24000,44100,48000]. default is 24000 */
  @Builder.Default Integer sampleRate = null;

  /** speechRate for tts , range [0.5~2.0],default is 1.0 */
  @Builder.Default Float speechRate = null;

  /** volume for tts , range [0~100],default is 50 */
  @Builder.Default Integer volume = null;

  /** format for tts, support mp3,wav,pcm,opus,default is pcm */
  @Builder.Default String format = null;

  /** pitchRate for tts , range [0.5~2.0],default is 1.0 */
  @Builder.Default Float pitchRate = null;

  /** bitRate for tts , support 6~510,default is 128kbps. only work on format: opus/mp3 */
  @Builder.Default Integer bitRate = null;

  /** The extra parameters. */
  @Builder.Default Map<String, Object> parameters = null;

  public JsonObject getConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(QwenTtsRealtimeConstants.VOICE, voice);
    config.put(QwenTtsRealtimeConstants.MODE, mode);
    if (this.format != null) {
      config.put(QwenTtsRealtimeConstants.RESPONSE_FORMAT, this.format);
    } else {
      config.put(QwenTtsRealtimeConstants.RESPONSE_FORMAT, responseFormat.getFormat());
    }
    if (this.sampleRate != null) {
      config.put(QwenTtsRealtimeConstants.SAMPLE_RATE, this.sampleRate);
    } else {
      config.put(QwenTtsRealtimeConstants.SAMPLE_RATE, responseFormat.getSampleRate());
    }
    if (this.speechRate != null) {
      config.put(QwenTtsRealtimeConstants.SPEECH_RATE, this.speechRate);
    }
    if (this.pitchRate != null) {
      config.put(QwenTtsRealtimeConstants.PITCH_RATE, this.pitchRate);
    }
    if (this.volume != null) {
      config.put(QwenTtsRealtimeConstants.VOLUME, this.volume);
    }
    if (this.bitRate != null) {
      config.put(QwenTtsRealtimeConstants.BIT_RATE, this.bitRate);
    }
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
