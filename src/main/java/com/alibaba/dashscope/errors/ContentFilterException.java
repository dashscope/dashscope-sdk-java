// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import java.util.Map;

/**
 * Input or output blocked by content moderation (wire code {@code DataInspectionFailed}). The
 * catalog entry behind it (content_policy_violation) names BadRequestException; refinement into a
 * dedicated subclass keeps moderation blocks catchable on their own — subclassing is the only
 * sanctioned refinement.
 */
public class ContentFilterException extends BadRequestException {
  public ContentFilterException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers) {
    super(message, code, statusCode, requestId, body, headers);
  }
}
