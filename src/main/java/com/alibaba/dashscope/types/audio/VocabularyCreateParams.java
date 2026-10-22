// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code audio().vocabularies().create}. {@code model} overrides the default
 * customization model ({@code speech-biasing}); unknown knobs pass through via {@link
 * Builder#parameters(Map)}.
 */
public final class VocabularyCreateParams {
  private final String targetModel;
  private final String prefix;
  private final List<Map<String, Object>> vocabulary;
  private final String model;
  private final Map<String, Object> parameters;

  private VocabularyCreateParams(Builder b) {
    this.targetModel = b.targetModel;
    this.prefix = b.prefix;
    this.vocabulary = b.vocabulary;
    this.model = b.model;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getTargetModel() {
    return targetModel;
  }

  public String getPrefix() {
    return prefix;
  }

  public List<Map<String, Object>> getVocabulary() {
    return vocabulary;
  }

  public String getModel() {
    return model;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String targetModel;
    private String prefix;
    private List<Map<String, Object>> vocabulary;
    private String model;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder targetModel(String targetModel) {
      this.targetModel = targetModel;
      return this;
    }

    public Builder prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public Builder vocabulary(List<Map<String, Object>> vocabulary) {
      this.vocabulary = vocabulary;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public VocabularyCreateParams build() {
      if (targetModel == null || targetModel.isEmpty()) {
        throw new IllegalArgumentException("targetModel is required");
      }
      if (prefix == null || prefix.isEmpty()) {
        throw new IllegalArgumentException("prefix is required");
      }
      if (vocabulary == null || vocabulary.isEmpty()) {
        throw new IllegalArgumentException("vocabulary must be a non-empty list");
      }
      return new VocabularyCreateParams(this);
    }
  }
}
