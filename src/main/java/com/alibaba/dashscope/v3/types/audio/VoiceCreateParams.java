// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code audio().voices().create} (voice cloning). Named fields win over the
 * {@link Builder#extraInput(Map)} escape hatch; {@code model} overrides the default customization
 * model ({@code voice-enrollment}).
 */
public final class VoiceCreateParams {
  private final String targetModel;
  private final String prefix;
  private final String url;
  private final List<String> languageHints;
  private final Double maxPromptAudioLength;
  private final Map<String, Object> extraInput;
  private final String model;
  private final Map<String, Object> parameters;

  private VoiceCreateParams(Builder b) {
    this.targetModel = b.targetModel;
    this.prefix = b.prefix;
    this.url = b.url;
    this.languageHints = b.languageHints;
    this.maxPromptAudioLength = b.maxPromptAudioLength;
    this.extraInput = b.extraInput;
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

  public String getUrl() {
    return url;
  }

  public List<String> getLanguageHints() {
    return languageHints;
  }

  public Double getMaxPromptAudioLength() {
    return maxPromptAudioLength;
  }

  public Map<String, Object> getExtraInput() {
    return extraInput;
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
    private String url;
    private List<String> languageHints;
    private Double maxPromptAudioLength;
    private Map<String, Object> extraInput;
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

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder languageHints(List<String> languageHints) {
      this.languageHints = languageHints;
      return this;
    }

    public Builder maxPromptAudioLength(Double maxPromptAudioLength) {
      this.maxPromptAudioLength = maxPromptAudioLength;
      return this;
    }

    public Builder extraInput(Map<String, Object> extraInput) {
      this.extraInput = extraInput;
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

    public VoiceCreateParams build() {
      if (targetModel == null || targetModel.isEmpty()) {
        throw new IllegalArgumentException("targetModel is required");
      }
      if (prefix == null || prefix.isEmpty()) {
        throw new IllegalArgumentException("prefix is required");
      }
      if (url == null || url.isEmpty()) {
        throw new IllegalArgumentException("url is required");
      }
      return new VoiceCreateParams(this);
    }
  }
}
