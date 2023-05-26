// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Status {

  /** The protocol (like http) status code */
  private int statusCode;

  /** The status message */
  private String message;

  /** The error code from the server side. */
  private String code;

  /** The message is in json format */
  @Builder.Default private boolean isJson = false;

  /** The usage information. */
  private JsonObject usage;

  /** The request id */
  private String requestId;
}
