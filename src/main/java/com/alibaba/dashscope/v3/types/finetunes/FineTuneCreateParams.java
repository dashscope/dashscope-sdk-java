// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.finetunes;

import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code fineTunes().create} (Python v2 parity). {@code finetunedModel} names the
 * produced model — it rides on the wire as {@code model_name} (the response-only {@code
 * finetuned_output} cannot be specified on create; current servers reject it).
 */
public final class FineTuneCreateParams {
  private final String model;
  private final List<String> trainingFileIds;
  private final List<String> validationFileIds;
  private final String mode;
  private final Map<String, Object> hyperParameters;
  private final String finetunedModel;

  private FineTuneCreateParams(Builder b) {
    this.model = b.model;
    this.trainingFileIds = b.trainingFileIds;
    this.validationFileIds = b.validationFileIds;
    this.mode = b.mode;
    this.hyperParameters = b.hyperParameters;
    this.finetunedModel = b.finetunedModel;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public List<String> getTrainingFileIds() {
    return trainingFileIds;
  }

  public List<String> getValidationFileIds() {
    return validationFileIds;
  }

  /** Training mode; rides on the wire as {@code training_type}. */
  public String getMode() {
    return mode;
  }

  public Map<String, Object> getHyperParameters() {
    return hyperParameters;
  }

  /** Name for the produced model; rides on the wire as {@code model_name}. */
  public String getFinetunedModel() {
    return finetunedModel;
  }

  public static final class Builder {
    private String model;
    private List<String> trainingFileIds;
    private List<String> validationFileIds;
    private String mode;
    private Map<String, Object> hyperParameters;
    private String finetunedModel;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder trainingFileIds(List<String> trainingFileIds) {
      this.trainingFileIds = trainingFileIds;
      return this;
    }

    public Builder validationFileIds(List<String> validationFileIds) {
      this.validationFileIds = validationFileIds;
      return this;
    }

    public Builder mode(String mode) {
      this.mode = mode;
      return this;
    }

    public Builder hyperParameters(Map<String, Object> hyperParameters) {
      this.hyperParameters = hyperParameters;
      return this;
    }

    public Builder finetunedModel(String finetunedModel) {
      this.finetunedModel = finetunedModel;
      return this;
    }

    public FineTuneCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (trainingFileIds == null || trainingFileIds.isEmpty()) {
        throw new IllegalArgumentException("training_file_ids is required");
      }
      return new FineTuneCreateParams(this);
    }
  }
}
