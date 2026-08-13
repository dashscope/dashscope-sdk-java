// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

/** Webhook delivery audit status. */
public enum WebhookDeliveryStatus {
  /** The delivery is waiting to start. */
  PENDING,

  /** The delivery is currently being sent. */
  DELIVERING,

  /** The delivery is waiting for its next retry. */
  WAITING_RETRY,

  /** The delivery completed successfully. */
  SUCCEEDED,

  /** The delivery exhausted all attempts. */
  FAILED,

  /** The delivery was canceled because its endpoint was disabled or deleted. */
  CANCELED
}
