// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * Session configuration for {@code RealtimeSession.updateSession} (audio design §1.3).
 *
 * <p>Only explicitly-set fields are serialized — unlike 2.x, unset fields are omitted rather than
 * sent with defaults. To force a JSON {@code null} on the wire (the explicit-null sentinel, e.g. to
 * clear a server-side setting), put a {@code null} value into {@link #parameters}; a typed field
 * that was never set simply does not appear.
 *
 * <p>{@code enableInputAudioTranscription}/{@code enableTurnDetection} follow Python semantics:
 * {@code TRUE} emits the config object, {@code FALSE} emits an explicit {@code null} (disables the
 * feature server-side), unset emits nothing.
 */
public final class UpdateSessionParams {
  private final List<String> modalities;
  private final String voice;
  private final String inputAudioFormat;
  private final String outputAudioFormat;
  private final AudioFormat inputAudio;
  private final AudioFormat outputAudio;
  private final String inputVideoRepresentationCompact;
  private final Boolean enableInputAudioTranscription;
  private final String inputAudioTranscriptionModel;
  private final Boolean enableTurnDetection;
  private final String turnDetectionType;
  private final Float turnDetectionThreshold;
  private final Integer prefixPaddingMs;
  private final Integer turnDetectionSilenceDurationMs;
  private final Map<String, Object> turnDetectionParam;
  private final Translation translation;
  private final Transcription transcription;
  private final Map<String, Object> parameters;

  private UpdateSessionParams(Builder b) {
    this.modalities = b.modalities;
    this.voice = b.voice;
    this.inputAudioFormat = b.inputAudioFormat;
    this.outputAudioFormat = b.outputAudioFormat;
    this.inputAudio = b.inputAudio;
    this.outputAudio = b.outputAudio;
    this.inputVideoRepresentationCompact = b.inputVideoRepresentationCompact;
    this.enableInputAudioTranscription = b.enableInputAudioTranscription;
    this.inputAudioTranscriptionModel = b.inputAudioTranscriptionModel;
    this.enableTurnDetection = b.enableTurnDetection;
    this.turnDetectionType = b.turnDetectionType;
    this.turnDetectionThreshold = b.turnDetectionThreshold;
    this.prefixPaddingMs = b.prefixPaddingMs;
    this.turnDetectionSilenceDurationMs = b.turnDetectionSilenceDurationMs;
    this.turnDetectionParam = b.turnDetectionParam;
    this.translation = b.translation;
    this.transcription = b.transcription;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Serializes the {@code session} object of a {@code session.update} frame. */
  public ObjectNode toSessionJson() {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode session = mapper.createObjectNode();
    if (modalities != null) {
      ArrayNode mods = session.putArray("modalities");
      for (String m : modalities) {
        mods.add(m);
      }
    }
    if (voice != null) {
      session.put("voice", voice);
    }
    if (inputAudio != null || outputAudio != null) {
      // New-style nested audio format; an unspecified side falls back to the legacy flat field,
      // then to the server-side default (Python parity).
      ObjectNode audio = session.putObject("audio");
      AudioFormat effectiveInput =
          inputAudio != null
              ? inputAudio
              : (inputAudioFormat != null ? AudioFormat.legacy(inputAudioFormat) : null);
      AudioFormat effectiveOutput =
          outputAudio != null
              ? outputAudio
              : (outputAudioFormat != null ? AudioFormat.legacy(outputAudioFormat) : null);
      if (effectiveInput != null) {
        audio.set("input", effectiveInput.toDirectionJson());
      }
      if (effectiveOutput != null) {
        audio.set("output", effectiveOutput.toDirectionJson());
      }
    } else {
      if (inputAudioFormat != null) {
        session.put("input_audio_format", inputAudioFormat);
      }
      if (outputAudioFormat != null) {
        session.put("output_audio_format", outputAudioFormat);
      }
    }
    if (inputVideoRepresentationCompact != null) {
      session
          .putObject("video")
          .putObject("input")
          .put("representation_compact", inputVideoRepresentationCompact);
    }
    if (enableInputAudioTranscription != null) {
      if (enableInputAudioTranscription) {
        ObjectNode transcriptionNode = session.putObject("input_audio_transcription");
        if (inputAudioTranscriptionModel != null) {
          transcriptionNode.put("model", inputAudioTranscriptionModel);
        }
      } else {
        session.putNull("input_audio_transcription");
      }
    }
    if (enableTurnDetection != null) {
      if (enableTurnDetection) {
        ObjectNode turnDetection = session.putObject("turn_detection");
        if (turnDetectionType != null) {
          turnDetection.put("type", turnDetectionType);
        }
        if (turnDetectionThreshold != null) {
          turnDetection.put("threshold", turnDetectionThreshold);
        }
        if (prefixPaddingMs != null) {
          turnDetection.put("prefix_padding_ms", prefixPaddingMs);
        }
        if (turnDetectionSilenceDurationMs != null) {
          turnDetection.put("silence_duration_ms", turnDetectionSilenceDurationMs);
        }
        merge(turnDetection, turnDetectionParam);
      } else {
        session.putNull("turn_detection");
      }
    }
    if (translation != null) {
      ObjectNode node = session.putObject("translation");
      if (translation.getLanguage() != null) {
        node.put("language", translation.getLanguage());
      }
      if (translation.getCorpus() != null) {
        node.set("corpus", mapper.valueToTree(translation.getCorpus()));
      }
    }
    if (transcription != null) {
      // qwen_audio dialect (2.x qwen-asr parity): language/corpus merge into
      // input_audio_transcription; format/sample_rate ride top-level — skipped when the nested
      // audio node is present to avoid conflicting formats (Python parity).
      com.fasterxml.jackson.databind.JsonNode existing = session.get("input_audio_transcription");
      ObjectNode node =
          existing instanceof ObjectNode
              ? (ObjectNode) existing
              : session.putObject("input_audio_transcription");
      if (transcription.getLanguage() != null) {
        node.put("language", transcription.getLanguage());
      }
      if (transcription.getCorpus() != null) {
        node.set("corpus", mapper.valueToTree(transcription.getCorpus()));
      }
      if (!session.has("audio")) {
        if (transcription.getInputAudioFormat() != null) {
          session.put("input_audio_format", transcription.getInputAudioFormat());
        }
        if (transcription.getSampleRate() != null) {
          session.put("sample_rate", transcription.getSampleRate());
        }
      }
    }
    merge(session, parameters);
    return session;
  }

  private static void merge(ObjectNode target, Map<String, Object> extra) {
    if (extra == null) {
      return;
    }
    ObjectMapper mapper = JsonSupport.mapper();
    for (Map.Entry<String, Object> e : extra.entrySet()) {
      if (e.getValue() == null) {
        // explicit-null sentinel: a null value in the passthrough map forces JSON null on the wire
        target.putNull(e.getKey());
      } else {
        target.set(e.getKey(), mapper.valueToTree(e.getValue()));
      }
    }
  }

  /** New-style nested audio format ({@code audio.input/output.format}). */
  public static final class AudioFormat {
    private final String type;
    private final Integer sampleRate;
    private final Map<String, Object> parameters;

    private AudioFormat(String type, Integer sampleRate, Map<String, Object> parameters) {
      this.type = type;
      this.sampleRate = sampleRate;
      this.parameters = parameters;
    }

    public static AudioFormat of(String type, Integer sampleRate) {
      return new AudioFormat(type, sampleRate, null);
    }

    public static AudioFormat of(String type, Integer sampleRate, Map<String, Object> parameters) {
      return new AudioFormat(type, sampleRate, parameters);
    }

    /** Maps a legacy flat format like {@code pcm16} onto the nested shape (pcm + sample rate). */
    static AudioFormat legacy(String flat) {
      if ("pcm16".equals(flat)) {
        return new AudioFormat("pcm", 16000, null);
      }
      if ("pcm24".equals(flat)) {
        return new AudioFormat("pcm", 24000, null);
      }
      return new AudioFormat(flat, null, null);
    }

    ObjectNode toDirectionJson() {
      ObjectMapper mapper = JsonSupport.mapper();
      ObjectNode format = mapper.createObjectNode();
      if (parameters != null) {
        for (Map.Entry<String, Object> e : parameters.entrySet()) {
          format.set(e.getKey(), mapper.valueToTree(e.getValue()));
        }
      }
      format.put("type", type);
      if (sampleRate != null) {
        format.put("sample_rate", sampleRate);
      }
      ObjectNode direction = mapper.createObjectNode();
      direction.set("format", format);
      return direction;
    }
  }

  /** Livetranslate dialect translation settings. */
  public static final class Translation {
    private final String language;
    private final Map<String, Object> corpus;

    private Translation(String language, Map<String, Object> corpus) {
      this.language = language;
      this.corpus = corpus;
    }

    public static Translation of(String language) {
      return new Translation(language, null);
    }

    public static Translation of(String language, Map<String, Object> corpus) {
      return new Translation(language, corpus);
    }

    public String getLanguage() {
      return language;
    }

    public Map<String, Object> getCorpus() {
      return corpus;
    }
  }

  /** qwen_audio dialect transcription settings. */
  public static final class Transcription {
    private final Integer sampleRate;
    private final String inputAudioFormat;
    private final String language;
    private final Map<String, Object> corpus;

    private Transcription(
        Integer sampleRate, String inputAudioFormat, String language, Map<String, Object> corpus) {
      this.sampleRate = sampleRate;
      this.inputAudioFormat = inputAudioFormat;
      this.language = language;
      this.corpus = corpus;
    }

    public static Builder builder() {
      return new Builder();
    }

    public Integer getSampleRate() {
      return sampleRate;
    }

    public String getInputAudioFormat() {
      return inputAudioFormat;
    }

    public String getLanguage() {
      return language;
    }

    public Map<String, Object> getCorpus() {
      return corpus;
    }

    public static final class Builder {
      private Integer sampleRate;
      private String inputAudioFormat;
      private String language;
      private Map<String, Object> corpus;

      private Builder() {}

      public Builder sampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
        return this;
      }

      public Builder inputAudioFormat(String inputAudioFormat) {
        this.inputAudioFormat = inputAudioFormat;
        return this;
      }

      public Builder language(String language) {
        this.language = language;
        return this;
      }

      public Builder corpus(Map<String, Object> corpus) {
        this.corpus = corpus;
        return this;
      }

      public Transcription build() {
        return new Transcription(sampleRate, inputAudioFormat, language, corpus);
      }
    }
  }

  public static final class Builder {
    private List<String> modalities;
    private String voice;
    private String inputAudioFormat;
    private String outputAudioFormat;
    private AudioFormat inputAudio;
    private AudioFormat outputAudio;
    private String inputVideoRepresentationCompact;
    private Boolean enableInputAudioTranscription;
    private String inputAudioTranscriptionModel;
    private Boolean enableTurnDetection;
    private String turnDetectionType;
    private Float turnDetectionThreshold;
    private Integer prefixPaddingMs;
    private Integer turnDetectionSilenceDurationMs;
    private Map<String, Object> turnDetectionParam;
    private Translation translation;
    private Transcription transcription;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder modalities(List<String> modalities) {
      this.modalities = modalities;
      return this;
    }

    public Builder voice(String voice) {
      this.voice = voice;
      return this;
    }

    /**
     * Legacy flat format (e.g. {@code pcm16}/{@code pcm24}); ignored when {@link #inputAudio} is
     * set.
     */
    public Builder inputAudioFormat(String inputAudioFormat) {
      this.inputAudioFormat = inputAudioFormat;
      return this;
    }

    /** Legacy flat format; ignored when {@link #outputAudio} is set. */
    public Builder outputAudioFormat(String outputAudioFormat) {
      this.outputAudioFormat = outputAudioFormat;
      return this;
    }

    public Builder inputAudio(AudioFormat inputAudio) {
      this.inputAudio = inputAudio;
      return this;
    }

    public Builder outputAudio(AudioFormat outputAudio) {
      this.outputAudio = outputAudio;
      return this;
    }

    public Builder inputVideoRepresentationCompact(String inputVideoRepresentationCompact) {
      this.inputVideoRepresentationCompact = inputVideoRepresentationCompact;
      return this;
    }

    public Builder enableInputAudioTranscription(Boolean enableInputAudioTranscription) {
      this.enableInputAudioTranscription = enableInputAudioTranscription;
      return this;
    }

    public Builder inputAudioTranscriptionModel(String inputAudioTranscriptionModel) {
      this.inputAudioTranscriptionModel = inputAudioTranscriptionModel;
      return this;
    }

    public Builder enableTurnDetection(Boolean enableTurnDetection) {
      this.enableTurnDetection = enableTurnDetection;
      return this;
    }

    public Builder turnDetectionType(String turnDetectionType) {
      this.turnDetectionType = turnDetectionType;
      return this;
    }

    public Builder turnDetectionThreshold(Float turnDetectionThreshold) {
      this.turnDetectionThreshold = turnDetectionThreshold;
      return this;
    }

    public Builder prefixPaddingMs(Integer prefixPaddingMs) {
      this.prefixPaddingMs = prefixPaddingMs;
      return this;
    }

    public Builder turnDetectionSilenceDurationMs(Integer turnDetectionSilenceDurationMs) {
      this.turnDetectionSilenceDurationMs = turnDetectionSilenceDurationMs;
      return this;
    }

    public Builder turnDetectionParam(Map<String, Object> turnDetectionParam) {
      this.turnDetectionParam = turnDetectionParam;
      return this;
    }

    public Builder translation(Translation translation) {
      this.translation = translation;
      return this;
    }

    public Builder transcription(Transcription transcription) {
      this.transcription = transcription;
      return this;
    }

    /** Passthrough merged last; a {@code null} value serializes as an explicit JSON null. */
    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public UpdateSessionParams build() {
      return new UpdateSessionParams(this);
    }
  }
}
