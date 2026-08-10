// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeploymentRun extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("deployment_id")
  private String deploymentId;

  @SerializedName("agent")
  private DeploymentAgentReference agent;

  @SerializedName("session_id")
  private String sessionId;

  @SerializedName("trigger_source")
  private String triggerSource;

  @SerializedName("status")
  private String status;

  @SerializedName("error")
  private DeploymentError error;

  @SerializedName("started_at")
  private String startedAt;

  @SerializedName("finished_at")
  private String finishedAt;
}
