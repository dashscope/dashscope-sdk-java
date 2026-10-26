// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.tingwu;

import java.util.Map;

/**
 * Parameters for {@code tingwu().jobs().create} (offline summarization as a long task). {@code
 * input} is the free-form task payload (file url, app scene, ...), sent verbatim.
 */
public final class TingWuJobParams {
  private final String model;
  private final Map<String, Object> input;
  private final Map<String, Object> parameters;

  private TingWuJobParams(Builder b) {
    this.model = b.model;
    this.input = b.input;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  /** Free-form task input, carried as {@code input} verbatim. */
  public Map<String, Object> getInput() {
    return input;
  }

  /** Passthrough into {@code parameters}; omitted from the body when empty. */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private Map<String, Object> input;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder input(Map<String, Object> input) {
      this.input = input;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public TingWuJobParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (input == null || input.isEmpty()) {
        throw new IllegalArgumentException("input is required");
      }
      return new TingWuJobParams(this);
    }
  }
}
