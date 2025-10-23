// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/** @author lengjiayi */
@SuperBuilder
@Data
public class OmniRealtimeConfig {
  /** omni output modalities to be used in session */
  @NonNull List<OmniRealtimeModality> modalities;

  /** voice to be used in session ,not need in qwen-asr-realtime*/
  @Builder.Default String voice = null;

  /** input audio format */
  @Builder.Default
  OmniRealtimeAudioFormat inputAudioFormat = OmniRealtimeAudioFormat.PCM_16000HZ_MONO_16BIT;
  /** output audio format */
  @Builder.Default
  OmniRealtimeAudioFormat outputAudioFormat = OmniRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT;
  /** enable transcription for input audio */
  @Builder.Default boolean enableInputAudioTranscription = true;
  /** model used for input audio transcription */
  @Builder.Default String InputAudioTranscription = null;
  /** enable turn detection */
  @Builder.Default boolean enableTurnDetection = true;
  /** turn detection type */
  @Builder.Default String turnDetectionType = "server_vad";
  /**
   * turn detection threshold, range [-1, 1] In a noisy environment, it may be necessary to increase
   * the threshold to reduce false detections In a quiet environment, it may be necessary to
   * decrease the threshold to improve sensitivity
   */
  @Builder.Default float turnDetectionThreshold = 0.2f;
  /** prefix speech duration to detect speech start */
  @Builder.Default int prefixPaddingMs = 300;
  /** duration of silence in milliseconds to detect turn, range [200, 6000] */
  @Builder.Default int turnDetectionSilenceDurationMs = 800;
  /** extra parameters for turn detection */
  @Builder.Default Map<String, Object> turnDetectionParam = null;
  /** The extra parameters. */
  @Builder.Default Map<String, Object> parameters = null;
  /** translation configuration */
  @Builder.Default
  OmniRealtimeTranslationParam translationConfig = null;
  /** transcription configuration */
  @Builder.Default
  OmniRealtimeTranscriptionParam transcriptionConfig = null;

  public JsonObject getConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(OmniRealtimeConstants.MODALITIES, modalities);
    config.put(OmniRealtimeConstants.VOICE, voice);
    config.put(OmniRealtimeConstants.INPUT_AUDIO_FORMAT, inputAudioFormat);
    config.put(OmniRealtimeConstants.OUTPUT_AUDIO_FORMAT, outputAudioFormat);
    if (enableInputAudioTranscription) {
      Map<String, Object> inputTranscriptionConfig = new HashMap<>();
      inputTranscriptionConfig.put(
          OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION_MODEL, InputAudioTranscription);
      config.put(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION, inputTranscriptionConfig);
    } else {
      config.put(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION, null);
    }
    if (enableTurnDetection) {
      Map<String, Object> turnDetectionConfig = new HashMap<>();
      turnDetectionConfig.put(OmniRealtimeConstants.TURN_DETECTION_TYPE, turnDetectionType);
      turnDetectionConfig.put(
          OmniRealtimeConstants.TURN_DETECTION_THRESHOLD, turnDetectionThreshold);
      turnDetectionConfig.put(OmniRealtimeConstants.PREFIX_PADDING_MS, prefixPaddingMs);
      turnDetectionConfig.put(
          OmniRealtimeConstants.SILENCE_DURATION_MS, turnDetectionSilenceDurationMs);
      if (turnDetectionParam != null) {
        for (Map.Entry<String, Object> entry : turnDetectionParam.entrySet()) {
          turnDetectionConfig.put(entry.getKey(), entry.getValue());
        }
      }
      config.put(OmniRealtimeConstants.TURN_DETECTION, turnDetectionConfig);
    } else {
      config.put(OmniRealtimeConstants.TURN_DETECTION, null);
    }
    // Add translation configuration to the config
    if (translationConfig != null) {
      Map<String, Object> translationConfig = new HashMap<>();
      translationConfig.put(OmniRealtimeConstants.LANGUAGE, this.translationConfig.getLanguage());
      config.put(OmniRealtimeConstants.TRANSLATION, translationConfig);
    } else {
      config.put(OmniRealtimeConstants.TRANSLATION, null);
    }
    // Add transcription configuration for qwen-asr-realtime
    if (transcriptionConfig != null) {
      Map<String, Object> transcriptionConfig = new HashMap<>();
      if (this.transcriptionConfig.getInputSampleRate() != null) {
        config.put(OmniRealtimeConstants.SAMPLE_RATE, this.transcriptionConfig.getInputSampleRate());
      }
      if (this.transcriptionConfig.getInputAudioFormat() != null) {
        config.put(OmniRealtimeConstants.INPUT_AUDIO_FORMAT, this.transcriptionConfig.getInputAudioFormat());
      }
      if (this.transcriptionConfig.getLanguage() != null) {
        transcriptionConfig.put(OmniRealtimeConstants.LANGUAGE, this.transcriptionConfig.getLanguage());
      }
      if (this.transcriptionConfig.getCorpus() != null) {
        transcriptionConfig.put(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION_CORPUS, this.transcriptionConfig.getCorpus());
      }
      config.put(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION, transcriptionConfig);
    }
    if (parameters != null) {
      for (Map.Entry<String, Object> entry : parameters.entrySet()) {
        config.put(entry.getKey(), entry.getValue());
      }
    }
    GsonBuilder builder = new GsonBuilder();
    builder.serializeNulls();
    Gson gson = builder.create();
    JsonObject jsonObject = gson.toJsonTree(config).getAsJsonObject();
    return jsonObject;
  }
}