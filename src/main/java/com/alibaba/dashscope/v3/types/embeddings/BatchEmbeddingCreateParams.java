// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.embeddings;

import java.util.Map;

/**
 * Parameters for {@code embeddings().jobs().create} (design §3, Python v2 EmbeddingJobs parity):
 * submit a batch task over one file whose lines are the texts. Singular {@code fileUrl}, unlike
 * transcription — this endpoint accepts exactly one file.
 */
public final class BatchEmbeddingCreateParams {
  private final String model;
  private final String fileUrl;
  private final String textType;
  private final Map<String, Object> parameters;

  private BatchEmbeddingCreateParams(Builder b) {
    this.model = b.model;
    this.fileUrl = b.fileUrl;
    this.textType = b.textType;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getFileUrl() {
    return fileUrl;
  }

  public String getTextType() {
    return textType;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String fileUrl;
    private String textType;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    /** URL of the batch input file (one text per line). */
    public Builder fileUrl(String fileUrl) {
      this.fileUrl = fileUrl;
      return this;
    }

    public Builder textType(String textType) {
      this.textType = textType;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public BatchEmbeddingCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (fileUrl == null || fileUrl.isEmpty()) {
        throw new IllegalArgumentException("file_url must not be empty");
      }
      return new BatchEmbeddingCreateParams(this);
    }
  }
}
