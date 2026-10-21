// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import java.util.Map;

/** HTTP 404: the requested resource was not found. */
public class NotFoundException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.RESOURCE_NOT_FOUND.getStatusCode();

  public NotFoundException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
