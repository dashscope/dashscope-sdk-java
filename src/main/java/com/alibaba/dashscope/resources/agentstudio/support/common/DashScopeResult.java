// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.common;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;

/**
 * Flatten-mode 3.0 copy of the 2.x DashScopeResult: the agentstudio endpoints answer flat JSON
 * bodies, so {@link #getOutput()} carries the whole response object (2.x {@code fromResponse(...,
 * isFlattenResult=true)} parity).
 */
@Data
public class DashScopeResult {
  private Object output;
  private String event;
  private String requestId;
  private Integer statusCode;
  private String code;
  private String message;
  private JsonObject usage;
  private Map<String, Object> headers;

  /**
   * Build from a successful HTTP exchange. An empty body (e.g. 204) yields an empty JSON object —
   * Gson cannot parse empty strings, and non-2xx responses never reach this path (they are raised
   * as ApiException upstream).
   */
  public static DashScopeResult fromHttpResponse(
      int httpStatus, Map<String, List<String>> responseHeaders, String body, String event) {
    DashScopeResult result = new DashScopeResult();
    result.headers = changeHeaders(responseHeaders);
    result.statusCode = httpStatus;
    result.event = event;
    if (body == null || body.isEmpty()) {
      result.output = new JsonObject();
    } else {
      result.output = result.parseJson(body);
    }
    return result;
  }

  private JsonObject parseJson(String raw) {
    try {
      return com.alibaba.dashscope.resources.agentstudio.support.utils.JsonUtils.parse(raw);
    } catch (Exception e) {
      // 2.x parity: an unparseable 2xx body surfaces as an internal-error result with null output.
      this.output = null;
      this.statusCode = 500;
      this.code = "InternalServerError";
      this.message = "The server encountered an internal error. Please try again later.";
      return null;
    }
  }

  private static Map<String, Object> changeHeaders(Map<String, List<String>> headers) {
    if (headers == null || headers.isEmpty()) {
      return null;
    }
    return headers.entrySet().stream()
        .filter(entry -> entry.getKey() != null)
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  List<String> values = entry.getValue();
                  return (values == null || values.isEmpty()) ? "" : String.join(",", values);
                },
                (v1, v2) -> v1,
                java.util.LinkedHashMap::new));
  }
}
