// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DeploymentPausedReason {
  @SerializedName("type")
  private String type;

  @SerializedName("error")
  private DeploymentError error;
}
