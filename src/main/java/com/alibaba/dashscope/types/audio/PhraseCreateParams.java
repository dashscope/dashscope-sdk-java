// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import java.util.Map;

/**
 * Parameters for {@code audio().phrases().create}/{@code update} (paraformer hot-phrase compile
 * jobs). {@code trainingType} defaults to {@code compile_asr_phrase}.
 */
public final class PhraseCreateParams {
  /** Default training type: compile_asr_phrase. */
  public static final String DEFAULT_TRAINING_TYPE = "compile_asr_phrase";

  private final String model;
  private final Map<String, Object> phrases;
  private final String trainingType;

  private PhraseCreateParams(Builder b) {
    this.model = b.model;
    this.phrases = b.phrases;
    this.trainingType = b.trainingType;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  /** The phrase table, sent as {@code hyper_parameters.phrase_list}. */
  public Map<String, Object> getPhrases() {
    return phrases;
  }

  public String getTrainingType() {
    return trainingType;
  }

  public static final class Builder {
    private String model;
    private Map<String, Object> phrases;
    private String trainingType = DEFAULT_TRAINING_TYPE;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder phrases(Map<String, Object> phrases) {
      this.phrases = phrases;
      return this;
    }

    public Builder trainingType(String trainingType) {
      this.trainingType = trainingType;
      return this;
    }

    public PhraseCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (phrases == null || phrases.isEmpty()) {
        throw new IllegalArgumentException("phrases must be a non-empty map");
      }
      return new PhraseCreateParams(this);
    }
  }
}
