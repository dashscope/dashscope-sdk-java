// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// This class-level matcher requires suppression here to preserve the diagnostic toString().
/**
 * Base class for all DashScope 3.0 SDK errors. Unchecked by design (2.x checked exceptions leaked
 * into every signature).
 */
@SuppressWarnings("OverrideThrowableToString")
public class DashScopeException extends RuntimeException {
  private final String code;
  private final Integer statusCode;
  private final String requestId;
  private final transient Object body;
  private final Map<String, String> headers;

  public DashScopeException(String message) {
    this(message, null, null, null, null, null, null);
  }

  public DashScopeException(String message, Throwable cause) {
    this(message, null, null, null, null, null, cause);
  }

  public DashScopeException(
      String message,
      String code,
      Integer statusCode,
      String requestId,
      Object body,
      Map<String, String> headers,
      Throwable cause) {
    super(message, cause);
    this.code = code;
    this.statusCode = statusCode;
    this.requestId = requestId;
    this.body = body;
    this.headers =
        headers == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
  }

  /** Public error code (e.g. {@code BadRequestError}) or wire code when unmapped; nullable. */
  public String getCode() {
    return code;
  }

  /** HTTP status of the failed exchange; null for SDK-side failures. */
  public Integer getStatusCode() {
    return statusCode;
  }

  public String getRequestId() {
    return requestId;
  }

  /** Raw response body (decoded JSON as Map/List, or raw text); nullable. */
  public Object getBody() {
    return body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append('<').append(getClass().getSimpleName()).append("> ").append(getMessage());
    if (code != null) {
      sb.append(" | code=").append(code);
    }
    if (statusCode != null) {
      sb.append(" | statusCode=").append(statusCode);
    }
    if (requestId != null) {
      sb.append(" | requestId=").append(requestId);
    }
    return sb.toString();
  }
}
