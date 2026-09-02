// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Delivery information attached to an endpoint event. */
@Data
public class WebhookDelivery {
  @SerializedName("webhook_id")
  private String webhookId;

  @SerializedName("status")
  private WebhookDeliveryStatus status;

  @SerializedName("attempt_count")
  private Integer attemptCount;

  @SerializedName("delivery_at")
  private String deliveryAt;

  @SerializedName("finish_at")
  private String finishAt;

  @SerializedName("failure_reason")
  private String failureReason;
}
