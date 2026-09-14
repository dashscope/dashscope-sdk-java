// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Managed Agent webhook endpoint. */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookEndpoint extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("description")
  private String description;

  @SerializedName("url")
  private String url;

  @SerializedName("events")
  private List<String> events;

  @SerializedName("status")
  private WebhookStatus status;

  @SerializedName("disabled_reason")
  private WebhookDisabledReason disabledReason;

  @SerializedName("consecutive_fail")
  private Integer consecutiveFail;

  @SerializedName("last_success_at")
  private String lastSuccessAt;

  @SerializedName("last_failure_at")
  private String lastFailureAt;

  @SerializedName("signing_secret")
  private String signingSecret;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("updated_at")
  private String updatedAt;
}
