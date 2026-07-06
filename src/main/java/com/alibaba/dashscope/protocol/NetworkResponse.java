// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NetworkResponse {

  /** The response headers. */
  @Builder.Default private Map<String, List<String>> headers = new HashMap<>();

  /** The String type response message. */
  private String message;

  /** The event, used in assistant */
  private String event;

  /** The binary type response. */
  private ByteBuffer binary;

  /** The HTTP status code */
  private Integer httpStatusCode;
}
