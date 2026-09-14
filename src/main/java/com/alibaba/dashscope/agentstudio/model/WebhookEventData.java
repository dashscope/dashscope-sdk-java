// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.agentstudio.model;

import com.google.gson.annotations.SerializedName;
import java.util.Map;
import lombok.Data;

/** Resource data carried by a webhook event envelope. */
@Data
public class WebhookEventData {
  @SerializedName("id")
  private String id;

  @SerializedName("type")
  private String type;

  @SerializedName("workspace_id")
  private String workspaceId;

  @SerializedName("session_thread_id")
  private String sessionThreadId;

  @SerializedName("vault_id")
  private String vaultId;

  @SerializedName("extensions")
  private Map<String, Object> extensions;
}
