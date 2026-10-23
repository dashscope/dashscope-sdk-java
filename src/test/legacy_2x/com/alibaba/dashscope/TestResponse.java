// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class TestResponse {
  @SerializedName("request_id")
  private String requestId;

  private String code;
  private String message;

  private JsonObject output;
  private JsonObject usage;
}
