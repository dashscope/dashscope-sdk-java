// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

/** Reason why a webhook endpoint was disabled. */
public enum WebhookDisabledReason {
  /** The endpoint was disabled manually. */
  MANUAL,

  /** The endpoint reached the consecutive delivery failure threshold. */
  CONSECUTIVE_DELIVERY_FAILURES,

  /** The endpoint returned an HTTP redirect response. */
  REDIRECT_RESPONSE,

  /** The endpoint failed server-side request forgery validation. */
  SSRF_VALIDATION_FAILED,

  /** The endpoint failed Transport Layer Security validation. */
  TLS_VALIDATION_FAILED
}
