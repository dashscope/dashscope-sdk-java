// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class DeploymentAgentParam {
  @NonNull
  @SerializedName("id")
  private String id;

  @SerializedName("version")
  private Integer version;
}
