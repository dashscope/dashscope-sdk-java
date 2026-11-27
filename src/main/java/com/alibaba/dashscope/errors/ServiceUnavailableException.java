// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import java.util.Map;

/** HTTP 503: the service is temporarily unavailable. Retryable per the retry policy. */
public class ServiceUnavailableException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.SERVICE_UNAVAILABLE.getStatusCode();

  public ServiceUnavailableException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
