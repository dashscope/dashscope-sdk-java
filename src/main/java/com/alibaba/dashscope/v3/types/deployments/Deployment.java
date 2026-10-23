// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.deployments;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One model deployment (Python v2 parity). The same type comes back from {@code create} / {@code
 * get} / {@code scale} and is nested in {@link DeploymentList#getDeployments()}, so there is one
 * shape to learn; the envelope rides along on the nested copies too, empty there because the server
 * reports it once per response.
 */
public final class Deployment extends ResponseBase {
  @JsonProperty("deployed_model")
  private String deployedModel;

  @JsonProperty("model")
  @JsonAlias("model_name")
  private String model;

  @JsonProperty("base_model")
  private String baseModel;

  @JsonProperty("status")
  private String status;

  @JsonProperty("capacity")
  private Integer capacity;

  @JsonProperty("base_capacity")
  private Integer baseCapacity;

  @JsonProperty("ready_capacity")
  private Integer readyCapacity;

  @JsonProperty("workspace_id")
  private String workspaceId;

  @JsonProperty("charge_type")
  private String chargeType;

  @JsonProperty("created_at")
  @JsonAlias("gmt_create")
  private String createdAt;

  @JsonProperty("updated_at")
  @JsonAlias("gmt_modified")
  private String updatedAt;

  @JsonProperty("modifier")
  private String modifier;

  @JsonProperty("creator")
  private String creator;

  public String getDeployedModel() {
    return deployedModel;
  }

  public String getModel() {
    return model;
  }

  public String getBaseModel() {
    return baseModel;
  }

  public String getStatus() {
    return status;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public Integer getBaseCapacity() {
    return baseCapacity;
  }

  public Integer getReadyCapacity() {
    return readyCapacity;
  }

  public String getWorkspaceId() {
    return workspaceId;
  }

  public String getChargeType() {
    return chargeType;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public String getModifier() {
    return modifier;
  }

  public String getCreator() {
    return creator;
  }
}
