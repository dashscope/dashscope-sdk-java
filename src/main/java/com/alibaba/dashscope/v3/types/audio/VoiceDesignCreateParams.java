// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import java.util.Map;

/**
 * Parameters for {@code audio().voiceDesign().create} (qwen voice design). Named fields win over
 * the {@link Builder#extraInput(Map)} escape hatch; {@code model} overrides the default
 * customization model ({@code qwen-voice-design}).
 */
public final class VoiceDesignCreateParams {
  private final String targetModel;
  private final String voicePrompt;
  private final String previewText;
  private final String preferredName;
  private final String language;
  private final Map<String, Object> extraInput;
  private final String model;
  private final Map<String, Object> parameters;

  private VoiceDesignCreateParams(Builder b) {
    this.targetModel = b.targetModel;
    this.voicePrompt = b.voicePrompt;
    this.previewText = b.previewText;
    this.preferredName = b.preferredName;
    this.language = b.language;
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

  public String getVoicePrompt() {
    return voicePrompt;
  }

  public String getPreviewText() {
    return previewText;
  }

  public String getPreferredName() {
    return preferredName;
  }

  public String getLanguage() {
    return language;
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
    private String voicePrompt;
    private String previewText;
    private String preferredName;
    private String language;
    private Map<String, Object> extraInput;
    private String model;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder targetModel(String targetModel) {
      this.targetModel = targetModel;
      return this;
    }

    public Builder voicePrompt(String voicePrompt) {
      this.voicePrompt = voicePrompt;
      return this;
    }

    public Builder previewText(String previewText) {
      this.previewText = previewText;
      return this;
    }

    public Builder preferredName(String preferredName) {
      this.preferredName = preferredName;
      return this;
    }

    public Builder language(String language) {
      this.language = language;
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

    public VoiceDesignCreateParams build() {
      if (targetModel == null || targetModel.isEmpty()) {
        throw new IllegalArgumentException("targetModel is required");
      }
      if (voicePrompt == null || voicePrompt.isEmpty()) {
        throw new IllegalArgumentException("voicePrompt is required");
      }
      if (previewText == null || previewText.isEmpty()) {
        throw new IllegalArgumentException("previewText is required");
      }
      return new VoiceDesignCreateParams(this);
    }
  }
}
