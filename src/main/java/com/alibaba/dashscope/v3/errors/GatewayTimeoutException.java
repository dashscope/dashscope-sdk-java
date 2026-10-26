// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.errors;

import java.util.Map;

/** HTTP 504: the gateway timed out. Retryable per the retry policy. */
public class GatewayTimeoutException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.REQUEST_TIMEOUT.getStatusCode();

  public GatewayTimeoutException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
