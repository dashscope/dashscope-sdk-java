// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.agentstudio.message.Message;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Deployment extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("name")
  private String name;

  @SerializedName("description")
  private String description;

  @SerializedName("agent")
  private Agent agent;

  @SerializedName("environment_id")
  private String environmentId;

  @SerializedName("schedule")
  private DeploymentSchedule schedule;

  @SerializedName("initial_events")
  private List<Message> initialEvents;

  @SerializedName("resources")
  private List<DeploymentResource> resources;

  @SerializedName("vault_ids")
  private List<String> vaultIds;

  @SerializedName("metadata")
  private Map<String, Object> metadata;

  @SerializedName("status")
  private String status;

  @SerializedName("paused_reason")
  private DeploymentPausedReason pausedReason;

  @SerializedName("archived_at")
  private String archivedAt;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;
}
