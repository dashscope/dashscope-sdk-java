// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class WebSocketResponseHeader {
  @SerializedName("task_id")
  public String taskId;

  public WebSocketEventType event;

  @SerializedName("error_code")
  public String code;

  @SerializedName("error_message")
  public String message;
}
