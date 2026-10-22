// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import java.util.Map;

/** HTTP 429: rate / concurrency / quota limit hit. Retryable per the retry policy. */
public class RateLimitException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.RATE_LIMIT_EXCEEDED.getStatusCode();

  public RateLimitException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
