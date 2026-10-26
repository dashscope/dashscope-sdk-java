// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import java.util.Map;

/** HTTP 400: the request is invalid. @see PublicErrorCode entries with errorCode BadRequestError */
public class BadRequestException extends ApiStatusException {
  /** Canonical status, referenced from the catalog (never a hand-written literal). */
  public static final int STATUS = PublicErrorCode.INVALID_REQUEST.getStatusCode();

  public BadRequestException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
