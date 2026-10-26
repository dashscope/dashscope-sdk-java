// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.tingwu;

import java.util.Map;

/**
 * Parameters for {@code tingwu().realtime} (ws duplex, task protocol). {@code format}/{@code
 * sampleRate} default to pcm/16000; named fields win over the {@link Builder#parameters(Map)}
 * escape hatch (1.x wire names: {@code format}/{@code sampleRate}/{@code terminology}/{@code
 * maxEndSilence}). {@code workspaceId}/{@code dataId} ride in the run-task {@code input}.
 */
public final class TingWuRealtimeParams {
  /** Default audio format: pcm. */
  public static final String DEFAULT_FORMAT = "pcm";

  /** Default sample rate: 16000. */
  public static final int DEFAULT_SAMPLE_RATE = 16000;

  private final String model;
  private final String appId;
  private final String format;
  private final Integer sampleRate;
  private final String terminology;
  private final Integer maxEndSilence;
  private final String workspaceId;
  private final String dataId;
  private final Map<String, Object> parameters;

  private TingWuRealtimeParams(Builder b) {
    this.model = b.model;
    this.appId = b.appId;
    this.format = b.format;
    this.sampleRate = b.sampleRate;
    this.terminology = b.terminology;
    this.maxEndSilence = b.maxEndSilence;
    this.workspaceId = b.workspaceId;
    this.dataId = b.dataId;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getAppId() {
    return appId;
  }

  public String getFormat() {
    return format;
  }

  public Integer getSampleRate() {
    return sampleRate;
  }

  public String getTerminology() {
    return terminology;
  }

  public Integer getMaxEndSilence() {
    return maxEndSilence;
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public String getDataId() {
    return dataId;
  }

  /** Passthrough into {@code payload.parameters}; named fields override it. */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String appId;
    private String format = DEFAULT_FORMAT;
    private Integer sampleRate = DEFAULT_SAMPLE_RATE;
    private String terminology;
    private Integer maxEndSilence;
    private String workspaceId;
    private String dataId;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder appId(String appId) {
      this.appId = appId;
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

    public Builder terminology(String terminology) {
      this.terminology = terminology;
      return this;
    }

    public Builder maxEndSilence(Integer maxEndSilence) {
      this.maxEndSilence = maxEndSilence;
      return this;
    }

    public Builder workspaceId(String workspaceId) {
      this.workspaceId = workspaceId;
      return this;
    }

    public Builder dataId(String dataId) {
      this.dataId = dataId;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public TingWuRealtimeParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (appId == null || appId.isEmpty()) {
        throw new IllegalArgumentException("appId is required");
      }
      return new TingWuRealtimeParams(this);
    }
  }
}
