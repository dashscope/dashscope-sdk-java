// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code audio.asr().transcribe} (HTTP async job). Wire shape: {@code {model,
 * input:{file_urls:[...]}, parameters:{...}, resources?}}.
 */
public final class AsrTranscribeParams {
  /**
   * Default transcription model (audio design §8 — code, not the stale Python docs, is
   * authoritative).
   */
  public static final String DEFAULT_MODEL = "qwen-audio-3.1-asr-flash-filetrans";

  private final String model;
  private final List<String> fileUrls;
  private final String phraseId;
  private final Map<String, Object> parameters;
  private final Map<String, Object> extraInput;

  private AsrTranscribeParams(Builder b) {
    this.model = b.model;
    this.fileUrls = b.fileUrls;
    this.phraseId = b.phraseId;
    this.parameters = b.parameters;
    this.extraInput = b.extraInput;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public List<String> getFileUrls() {
    return fileUrls;
  }

  public String getPhraseId() {
    return phraseId;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public Map<String, Object> getExtraInput() {
    return extraInput;
  }

  public static final class Builder {
    private String model = DEFAULT_MODEL;
    private List<String> fileUrls;
    private String phraseId;
    private Map<String, Object> parameters;
    private Map<String, Object> extraInput;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder fileUrls(List<String> fileUrls) {
      this.fileUrls = fileUrls == null ? null : new ArrayList<>(fileUrls);
      return this;
    }

    public Builder phraseId(String phraseId) {
      this.phraseId = phraseId;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public Builder extraInput(Map<String, Object> extraInput) {
      this.extraInput = extraInput;
      return this;
    }

    public AsrTranscribeParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (fileUrls == null || fileUrls.isEmpty()) {
        throw new IllegalArgumentException("fileUrls must not be empty");
      }
      return new AsrTranscribeParams(this);
    }
  }
}
