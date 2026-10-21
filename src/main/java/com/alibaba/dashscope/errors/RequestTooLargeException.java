// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import java.util.Map;

/** HTTP 413: the request exceeds the maximum allowed size. */
public class RequestTooLargeException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.REQUEST_TOO_LARGE.getStatusCode();

  public RequestTooLargeException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
