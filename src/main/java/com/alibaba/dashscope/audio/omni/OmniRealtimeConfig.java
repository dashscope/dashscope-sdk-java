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

  /** voice to be used in session ,not need in qwen-asr-realtime */
  @Builder.Default String voice = null;

  /**
   * input audio format (legacy). Only supports pcm16/pcm24. Ignored when {@link #inputAudio} is
   * set.
   */
  @Builder.Default
  OmniRealtimeAudioFormat inputAudioFormat = OmniRealtimeAudioFormat.PCM_16000HZ_MONO_16BIT;
  /**
   * output audio format (legacy). Only supports pcm16/pcm24. Ignored when {@link #outputAudio} is
   * set.
   */
  @Builder.Default
  OmniRealtimeAudioFormat outputAudioFormat = OmniRealtimeAudioFormat.PCM_24000HZ_MONO_16BIT;
  /**
   * New-style input(upstream) audio format, supports "pcm"/"wav" format and 8k/16k/24k/48k sample
   * rate, e.g. {@code new OmniRealtimeAudioFormatConfig("pcm", 16000)}. When set, this takes
   * precedence over the legacy {@link #inputAudioFormat} and will be serialized as the nested
   * {@code audio.input.format} structure instead of the legacy flat {@code input_audio_format}
   * field. Setting only one of {@link #inputAudio}/{@link #outputAudio} is fine, the other side
   * falls back to its legacy field value.
   */
  @Builder.Default OmniRealtimeAudioFormatConfig inputAudio = null;
  /**
   * New-style output(downstream) audio format, see {@link #inputAudio}. When set, this takes
   * precedence over the legacy {@link #outputAudioFormat} and will be serialized as the nested
   * {@code audio.output.format} structure instead of the legacy flat {@code output_audio_format}
   * field.
   */
  @Builder.Default OmniRealtimeAudioFormatConfig outputAudio = null;
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
  @Builder.Default OmniRealtimeTranslationParam translationConfig = null;
  /** transcription configuration */
  @Builder.Default OmniRealtimeTranscriptionParam transcriptionConfig = null;

  public JsonObject getConfig() {
    Map<String, Object> config = new HashMap<>();
    config.put(OmniRealtimeConstants.MODALITIES, modalities);
    if (voice != null) {
      config.put(OmniRealtimeConstants.VOICE, voice);
    }
    if (inputAudio != null || outputAudio != null) {
      // New-style nested audio format, takes precedence over the legacy flat fields. The side
      // that is not explicitly set falls back to the legacy inputAudioFormat/outputAudioFormat
      // value so that the resulting "audio" node is always complete and consistent.
      OmniRealtimeAudioFormatConfig effectiveInputAudio =
          inputAudio != null
              ? inputAudio
              : new OmniRealtimeAudioFormatConfig(
                  OmniRealtimeAudioCodec.PCM, inputAudioFormat.getSampleRate());
      OmniRealtimeAudioFormatConfig effectiveOutputAudio =
          outputAudio != null
              ? outputAudio
              : new OmniRealtimeAudioFormatConfig(
                  OmniRealtimeAudioCodec.PCM, outputAudioFormat.getSampleRate());
      Map<String, Object> audio = new HashMap<>();
      audio.put(OmniRealtimeConstants.AUDIO_INPUT, buildAudioDirectionNode(effectiveInputAudio));
      audio.put(OmniRealtimeConstants.AUDIO_OUTPUT, buildAudioDirectionNode(effectiveOutputAudio));
      config.put(OmniRealtimeConstants.AUDIO, audio);
    } else {
      // Legacy flat fields, kept unchanged for full backward compatibility.
      config.put(OmniRealtimeConstants.INPUT_AUDIO_FORMAT, inputAudioFormat);
      config.put(OmniRealtimeConstants.OUTPUT_AUDIO_FORMAT, outputAudioFormat);
    }
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
      if (this.translationConfig.getCorpus() != null) {
        translationConfig.put(
            OmniRealtimeConstants.TRANSLATION_CORPUS, this.translationConfig.getCorpus());
      }
      config.put(OmniRealtimeConstants.TRANSLATION, translationConfig);
    }
    // Add transcription configuration for qwen-asr-realtime
    if (transcriptionConfig != null) {
      Map<String, Object> transcriptionConfig = new HashMap<>();
      if (this.transcriptionConfig.getInputSampleRate() != null) {
        config.put(
            OmniRealtimeConstants.SAMPLE_RATE, this.transcriptionConfig.getInputSampleRate());
      }
      if (this.transcriptionConfig.getInputAudioFormat() != null) {
        config.put(
            OmniRealtimeConstants.INPUT_AUDIO_FORMAT,
            this.transcriptionConfig.getInputAudioFormat());
      }
      if (this.transcriptionConfig.getLanguage() != null) {
        transcriptionConfig.put(
            OmniRealtimeConstants.LANGUAGE, this.transcriptionConfig.getLanguage());
      }
      if (this.transcriptionConfig.getCorpus() != null) {
        transcriptionConfig.put(
            OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION_CORPUS,
            this.transcriptionConfig.getCorpus());
      }
      Object existingConfig = config.get(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION);
      if (existingConfig instanceof Map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> tempMap = (Map<String, Object>) existingConfig;
        tempMap.putAll(transcriptionConfig);
        config.put(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION, tempMap);
      } else {
        config.put(OmniRealtimeConstants.INPUT_AUDIO_TRANSCRIPTION, transcriptionConfig);
      }
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

  /**
   * Builds the {@code { "format": { "type": ..., "sample_rate": ... } } } node used under {@code
   * audio.input} / {@code audio.output}.
   */
  private Map<String, Object> buildAudioDirectionNode(OmniRealtimeAudioFormatConfig config) {
    Map<String, Object> format = new HashMap<>();
    // Merge extra parameters first so that the typed fields below always take precedence and
    // can't be accidentally overridden by reserved keys (type/sample_rate).
    if (config.getParameters() != null) {
      format.putAll(config.getParameters());
    }
    format.put(OmniRealtimeConstants.AUDIO_FORMAT_TYPE, config.getType());
    format.put(OmniRealtimeConstants.SAMPLE_RATE, config.getSampleRate());
    Map<String, Object> direction = new HashMap<>();
    direction.put(OmniRealtimeConstants.AUDIO_FORMAT, format);
    return direction;
  }
}
