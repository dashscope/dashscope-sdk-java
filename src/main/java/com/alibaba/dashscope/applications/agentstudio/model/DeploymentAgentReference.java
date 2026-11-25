// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DeploymentAgentReference {
  @SerializedName("id")
  private String id;

  @SerializedName("version")
  private Integer version;
}
