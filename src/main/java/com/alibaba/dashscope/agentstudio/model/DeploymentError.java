// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DeploymentError {
  @SerializedName("code")
  private String code;

  @SerializedName("message")
  private String message;
}
