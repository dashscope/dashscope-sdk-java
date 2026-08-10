// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.param;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class DeploymentScheduleParam {
  public static final String TYPE_CRON = "cron";

  @NonNull
  @SerializedName("type")
  private String type;

  @NonNull
  @SerializedName("expression")
  private String expression;

  @NonNull
  @SerializedName("timezone")
  private String timezone;
}
