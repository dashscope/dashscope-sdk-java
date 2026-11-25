// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Raw result of one HTTP exchange after error policy ran: status + headers + decoded body. */
public final class ResponseEnvelope {
  private final int statusCode;
  private final Map<String, String> headers;
  private final JsonNode body;

  public ResponseEnvelope(int statusCode, Map<String, String> headers, JsonNode body) {
    this.statusCode = statusCode;
    this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    this.body = body;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  /** Decoded response body; null when the response carried no body. */
  public JsonNode getBody() {
    return body;
  }
}
