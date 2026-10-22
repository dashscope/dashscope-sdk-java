// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.multimodal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code multimodal.embeddings.create}. Wire shape: {@code {model,
 * input:{contents:[{text|image|audio|video:...}, ...]}, parameters:{...}}}.
 */
public final class MultimodalEmbeddingCreateParams {
  private final String model;
  private final List<ContentPart> contents;
  private final Map<String, Object> parameters;
  private final Map<String, Object> extraInput;

  private MultimodalEmbeddingCreateParams(Builder b) {
    this.model = b.model;
    this.contents = b.contents;
    this.parameters = b.parameters;
    this.extraInput = b.extraInput;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public List<ContentPart> getContents() {
    return contents;
  }

  /** Passthrough into the wire {@code parameters} object. */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /** Passthrough into the wire {@code input} object. */
  public Map<String, Object> getExtraInput() {
    return extraInput;
  }

  public static final class Builder {
    private String model;
    private List<ContentPart> contents;
    private Map<String, Object> parameters;
    private Map<String, Object> extraInput;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder contents(List<ContentPart> contents) {
      this.contents = contents == null ? null : new ArrayList<>(contents);
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

    public MultimodalEmbeddingCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (contents == null || contents.isEmpty()) {
        throw new IllegalArgumentException("contents must not be empty");
      }
      return new MultimodalEmbeddingCreateParams(this);
    }
  }
}
