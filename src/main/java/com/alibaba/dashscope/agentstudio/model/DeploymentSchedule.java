// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DeploymentSchedule {
  @SerializedName("type")
  private String type;

  @SerializedName("expression")
  private String expression;

  @SerializedName("timezone")
  private String timezone;

  @SerializedName("last_run_at")
  private String lastRunAt;

  @SerializedName("next_run_at")
  private String nextRunAt;
}
