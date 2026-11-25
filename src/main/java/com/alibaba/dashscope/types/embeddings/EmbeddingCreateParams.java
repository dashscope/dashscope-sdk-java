// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.embeddings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code embeddings.create}. Wire shape: {@code {model, input:{texts},
 * parameters:{...}}}.
 */
public final class EmbeddingCreateParams {
  private final String model;
  private final List<String> texts;
  private final String textType;
  private final Integer dimension;
  private final Map<String, Object> parameters;

  private EmbeddingCreateParams(Builder b) {
    this.model = b.model;
    this.texts = b.texts;
    this.textType = b.textType;
    this.dimension = b.dimension;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public List<String> getTexts() {
    return texts;
  }

  /** "query" or "document"; model-dependent. */
  public String getTextType() {
    return textType;
  }

  public Integer getDimension() {
    return dimension;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private List<String> texts;
    private String textType;
    private Integer dimension;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder texts(List<String> texts) {
      this.texts = texts == null ? null : new ArrayList<>(texts);
      return this;
    }

    public Builder text(String text) {
      this.texts = text == null ? null : new ArrayList<>(Collections.singletonList(text));
      return this;
    }

    public Builder textType(String textType) {
      this.textType = textType;
      return this;
    }

    public Builder dimension(Integer dimension) {
      this.dimension = dimension;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public EmbeddingCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (texts == null || texts.isEmpty()) {
        throw new IllegalArgumentException("texts must not be empty");
      }
      return new EmbeddingCreateParams(this);
    }
  }
}
