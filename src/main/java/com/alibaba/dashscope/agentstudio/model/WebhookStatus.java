// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

/** Webhook endpoint activation status. */
public enum WebhookStatus {
  /** The endpoint can receive webhook deliveries. */
  ACTIVE,

  /** The endpoint does not receive webhook deliveries. */
  DISABLED
}
