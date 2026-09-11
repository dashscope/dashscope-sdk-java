// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Webhook event envelope returned by test and endpoint event APIs. */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookEvent extends FlattenResultBase {
  @SerializedName("type")
  private String type;

  @SerializedName("id")
  private String id;

  @SerializedName("created_at")
  private String createdAt;

  @SerializedName("data")
  private WebhookEventData data;

  @SerializedName("delivery")
  private WebhookDelivery delivery;
}
