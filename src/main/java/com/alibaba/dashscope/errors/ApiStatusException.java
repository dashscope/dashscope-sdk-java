// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import java.util.Map;

/**
 * A failure the gateway declared, on either layer of the dual error model: a non-2xx HTTP status,
 * or HTTP 200 whose body carries a business {@code code}. The instance's {@code statusCode} says
 * which one it was. Unmapped wire codes land here directly (never guessed into a subclass).
 */
public class ApiStatusException extends DashScopeException {
  public ApiStatusException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers, null);
  }
}
