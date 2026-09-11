// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.alibaba.dashscope.common.FlattenResultBase;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Result returned after resetting a webhook signing secret. */
@Data
@EqualsAndHashCode(callSuper = true)
public class WebhookSecretReset extends FlattenResultBase {
  @SerializedName("id")
  private String id;

  @SerializedName("signing_secret")
  private String signingSecret;

  @SerializedName("updated_at")
  private String updatedAt;
}
