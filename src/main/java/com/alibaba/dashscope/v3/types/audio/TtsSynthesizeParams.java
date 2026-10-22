// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import java.util.Map;

/**
 * Parameters for the HTTP TTS forms ({@code audio.tts().synthesize/stream} and {@code
 * audio.tts().http().*}). Wire shape: {@code {model, input:{text, voice, format, sample_rate,
 * volume, rate, pitch, ...}}} — every speech knob lives INSIDE {@code input} (2.x
 * HttpSpeechSynthesisParam parity).
 */
public final class TtsSynthesizeParams {
  private final String model;
  private final String text;
  private final String voice;
  private final String format;
  private final Integer sampleRate;
  private final Integer volume;
  private final Float rate;
  private final Float pitch;
  private final Map<String, Object> parameters;

  private TtsSynthesizeParams(Builder b) {
    this.model = b.model;
    this.text = b.text;
    this.voice = b.voice;
    this.format = b.format;
    this.sampleRate = b.sampleRate;
    this.volume = b.volume;
    this.rate = b.rate;
    this.pitch = b.pitch;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getText() {
    return text;
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

  public Float getRate() {
    return rate;
  }

  public Float getPitch() {
    return pitch;
  }

  /** Passthrough merged INTO {@code input} (not {@code parameters}) — 2.x parity. */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String text;
    private String voice;
    private String format;
    private Integer sampleRate;
    private Integer volume;
    private Float rate;
    private Float pitch;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder text(String text) {
      this.text = text;
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

    public Builder rate(Float rate) {
      this.rate = rate;
      return this;
    }

    public Builder pitch(Float pitch) {
      this.pitch = pitch;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public TtsSynthesizeParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (text == null || text.isEmpty()) {
        throw new IllegalArgumentException("text is required");
      }
      return new TtsSynthesizeParams(this);
    }
  }
}
