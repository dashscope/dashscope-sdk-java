// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.common;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base of every 3.0 response (design §3): the server envelope flattened — requestId, statusCode,
 * code, message, headers — plus {@link #isError()} for {@code raiseOnError(false)} mode (2.x
 * return-value style).
 */
public class ResponseBase {
  private String requestId;
  private int statusCode;
  private String code;
  private String message;
  private Map<String, String> headers = Collections.emptyMap();

  /** Envelope fill is SDK-internal wiring (transport → response); users must not call it. */
  public void fillEnvelope(
      String requestId, int statusCode, String code, String message, Map<String, String> headers) {
    this.requestId = requestId;
    this.statusCode = statusCode;
    this.code = code;
    this.message = message;
    this.headers =
        headers == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
  }

  public String getRequestId() {
    return requestId;
  }

  /** HTTP status of the exchange (200 also for the 200-with-business-code error layer). */
  public int getStatusCode() {
    return statusCode;
  }

  /** Business error code; null/absent on success. */
  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  /** true when the response carries a business error code (raiseOnError=false mode). */
  public boolean isError() {
    return code != null && !code.isEmpty();
  }
}
