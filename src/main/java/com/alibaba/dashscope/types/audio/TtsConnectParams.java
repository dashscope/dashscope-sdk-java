// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import java.util.Map;

/**
 * Parameters for {@code audio.tts().connect} (ws duplex, task protocol). Known fields land in
 * {@code payload.parameters} of the run-task frame.
 */
public final class TtsConnectParams {
  private final String model;
  private final String voice;
  private final String format;
  private final Integer sampleRate;
  private final Integer volume;
  private final Float speechRate;
  private final Float pitchRate;
  private final Boolean enableSsml;
  private final String preTaskId;
  private final Map<String, Object> parameters;

  private TtsConnectParams(Builder b) {
    this.model = b.model;
    this.voice = b.voice;
    this.format = b.format;
    this.sampleRate = b.sampleRate;
    this.volume = b.volume;
    this.speechRate = b.speechRate;
    this.pitchRate = b.pitchRate;
    this.enableSsml = b.enableSsml;
    this.preTaskId = b.preTaskId;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getVoice() {
    return voice;
  }

  public String getFormat() {
    return format;
  }

  public Integer getSampleRate() {
    return sampleRate;
  }

  public Integer getVolume() {
    return volume;
  }

  public Float getSpeechRate() {
    return speechRate;
  }

  public Float getPitchRate() {
    return pitchRate;
  }

  public Boolean getEnableSsml() {
    return enableSsml;
  }

  public String getPreTaskId() {
    return preTaskId;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String voice;
    private String format;
    private Integer sampleRate;
    private Integer volume;
    private Float speechRate;
    private Float pitchRate;
    private Boolean enableSsml;
    private String preTaskId;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder voice(String voice) {
      this.voice = voice;
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

    public Builder volume(Integer volume) {
      this.volume = volume;
      return this;
    }

    public Builder speechRate(Float speechRate) {
      this.speechRate = speechRate;
      return this;
    }

    public Builder pitchRate(Float pitchRate) {
      this.pitchRate = pitchRate;
      return this;
    }

    public Builder enableSsml(Boolean enableSsml) {
      this.enableSsml = enableSsml;
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

    public TtsConnectParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      return new TtsConnectParams(this);
    }
  }
}
