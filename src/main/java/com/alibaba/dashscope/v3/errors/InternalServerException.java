// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import java.util.Map;

/** HTTP 500: the server encountered an internal error. Retryable per the retry policy. */
public class InternalServerException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.INTERNAL_ERROR.getStatusCode();

  public InternalServerException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
