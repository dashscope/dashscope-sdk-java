// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import java.util.Map;

/**
 * Parameters for {@code audio.asr().recognize} (ws duplex, task protocol). Known fields land in
 * {@code payload.parameters}; {@code pre_task_id} additionally pins the header task_id (2.x
 * parity); {@code phraseId} maps to top-level {@code resources}, never into parameters.
 * Unknown knobs pass through via {@link Builder#parameters(Map)}.
 */
public final class AsrRecognizeParams {
  private final String model;
  private final String format;
  private final Integer sampleRate;
  private final String vocabularyId;
  private final Boolean disfluencyRemovalEnabled;
  private final String phraseId;
  private final String preTaskId;
  private final Map<String, Object> parameters;

  private AsrRecognizeParams(Builder b) {
    this.model = b.model;
    this.format = b.format;
    this.sampleRate = b.sampleRate;
    this.vocabularyId = b.vocabularyId;
    this.disfluencyRemovalEnabled = b.disfluencyRemovalEnabled;
    this.phraseId = b.phraseId;
    this.preTaskId = b.preTaskId;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getFormat() {
    return format;
  }

  public Integer getSampleRate() {
    return sampleRate;
  }

  public String getVocabularyId() {
    return vocabularyId;
  }

  public Boolean getDisfluencyRemovalEnabled() {
    return disfluencyRemovalEnabled;
  }

  public String getPhraseId() {
    return phraseId;
  }

  public String getPreTaskId() {
    return preTaskId;
  }

  /** Passthrough into {@code payload.parameters}. */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String format;
    private Integer sampleRate;
    private String vocabularyId;
    private Boolean disfluencyRemovalEnabled;
    private String phraseId;
    private String preTaskId;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder format(String format) {
      this.format = format;
      return this;
    }

    public Builder sampleRate(Integer sampleRate) {
      this.sampleRate = sampleRate;
      return this;
    }

    public Builder vocabularyId(String vocabularyId) {
      this.vocabularyId = vocabularyId;
      return this;
    }

    public Builder disfluencyRemovalEnabled(Boolean disfluencyRemovalEnabled) {
      this.disfluencyRemovalEnabled = disfluencyRemovalEnabled;
      return this;
    }

    public Builder phraseId(String phraseId) {
      this.phraseId = phraseId;
      return this;
    }

    public Builder preTaskId(String preTaskId) {
      this.preTaskId = preTaskId;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public AsrRecognizeParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      return new AsrRecognizeParams(this);
    }
  }
}
