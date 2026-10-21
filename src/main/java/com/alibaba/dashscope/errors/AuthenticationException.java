// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import java.util.Map;

/** HTTP 401: authentication failed or API key invalid. */
public class AuthenticationException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.AUTH_FAILED.getStatusCode();

  public AuthenticationException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
