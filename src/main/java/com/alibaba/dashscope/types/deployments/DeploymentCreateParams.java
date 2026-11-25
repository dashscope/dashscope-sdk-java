// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.deployments;

import java.util.Map;

/**
 * Parameters for {@code deployments().create} (Python v2 parity). {@code model} and {@code
 * modelVersion} keep their public names; on the wire they spell {@code model_name} / {@code
 * model_version}. MU deployments reject the create call unless both {@code deploySpec} and {@code
 * billingMethod} are present.
 */
public final class DeploymentCreateParams {
  private final String model;
  private final Integer capacity;
  private final String modelVersion;
  private final String suffix;
  private final String plan;
  private final String templateId;
  private final Map<String, Object> deploySpec;
  private final String billingMethod;

  private DeploymentCreateParams(Builder b) {
    this.model = b.model;
    this.capacity = b.capacity;
    this.modelVersion = b.modelVersion;
    this.suffix = b.suffix;
    this.plan = b.plan;
    this.templateId = b.templateId;
    this.deploySpec = b.deploySpec;
    this.billingMethod = b.billingMethod;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public String getSuffix() {
    return suffix;
  }

  public String getPlan() {
    return plan;
  }

  public String getTemplateId() {
    return templateId;
  }

  public Map<String, Object> getDeploySpec() {
    return deploySpec;
  }

  public String getBillingMethod() {
    return billingMethod;
  }

  public static final class Builder {
    private String model;
    private Integer capacity;
    private String modelVersion;
    private String suffix;
    private String plan;
    private String templateId;
    private Map<String, Object> deploySpec;
    private String billingMethod;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder capacity(Integer capacity) {
      this.capacity = capacity;
      return this;
    }

    public Builder modelVersion(String modelVersion) {
      this.modelVersion = modelVersion;
      return this;
    }

    public Builder suffix(String suffix) {
      this.suffix = suffix;
      return this;
    }

    public Builder plan(String plan) {
      this.plan = plan;
      return this;
    }

    public Builder templateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder deploySpec(Map<String, Object> deploySpec) {
      this.deploySpec = deploySpec;
      return this;
    }

    public Builder billingMethod(String billingMethod) {
      this.billingMethod = billingMethod;
      return this;
    }

    public DeploymentCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (capacity == null) {
        throw new IllegalArgumentException("capacity is required");
      }
      return new DeploymentCreateParams(this);
    }
  }
}
