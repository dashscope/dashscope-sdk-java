// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import com.alibaba.dashscope.common.PublicErrorCode;
import java.util.Map;

/** HTTP 403: no permission to access the resource. */
public class PermissionDeniedException extends ApiStatusException {
  public static final int STATUS = PublicErrorCode.PERMISSION_DENIED.getStatusCode();

  public PermissionDeniedException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
