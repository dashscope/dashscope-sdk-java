// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol;

import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.applications.agentstudio.utils.ApiKey;
import com.alibaba.dashscope.applications.agentstudio.utils.StringUtils;
import com.alibaba.dashscope.shared.SdkInfo;
import com.alibaba.dashscope.shared.SdkSession;
import java.util.HashMap;
import java.util.Map;

/** Header construction for the AgentStudio HTTP paths. */
public final class DashScopeHeaders {
  private static final String SDK_CLIENT = "java-sdk";
  // Session id single-sourced from the neutral layer, shared with 3.0 in-process.
  private static final String SDK_SESSION_ID = SdkSession.ID;

  private DashScopeHeaders() {}

  public static String findRequestId(Map<String, ?> headers) {
    if (headers == null) {
      return null;
    }
    for (Map.Entry<String, ?> entry : headers.entrySet()) {
      String name = entry.getKey();
      if (name == null
          || !("x-request-id".equalsIgnoreCase(name)
              || "x-dashscope-requestid".equalsIgnoreCase(name)
              || "x-dashscope-request-id".equalsIgnoreCase(name))) {
        continue;
      }
      Object value = entry.getValue();
      if (value instanceof Iterable) {
        for (Object item : (Iterable<?>) value) {
          if (item != null && !item.toString().isEmpty()) {
            return item.toString();
          }
        }
      } else if (value != null && !value.toString().isEmpty()) {
        return value.toString();
      }
    }
    return null;
  }

  public static String requestIdOrUnknown(String requestId) {
    return requestId == null || requestId.isEmpty() ? "unknown" : requestId;
  }

  public static String userAgent() {
    return userAgent(null);
  }

  // Generate user agent with optional custom suffix
  public static String userAgent(String customUserAgent) {
    String userAgent =
        StringUtils.format(
            "dashscope/%s; java/%s; platform/%s; processor/%s",
            SdkInfo.VERSION,
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.arch"));
    if (customUserAgent != null && !customUserAgent.isEmpty()) {
      userAgent += "; " + customUserAgent;
    }
    return userAgent;
  }

  /** Check if SDK tracking headers are disabled via DASHSCOPE_DISABLE_SDK_HEADERS env var. */
  private static boolean isSdkHeadersDisabled() {
    String disable = System.getenv("DASHSCOPE_DISABLE_SDK_HEADERS");
    return "1".equals(disable) || "true".equalsIgnoreCase(disable);
  }

  /**
   * Build the combined sdk-client value: {@code client/version[/module]}. Module segment is omitted
   * when module is null or empty.
   */
  private static String buildSdkClientValue(String module) {
    String value = SDK_CLIENT + "/" + SdkInfo.VERSION;
    if (module != null && !module.isEmpty()) {
      value += "/" + module;
    }
    return value;
  }

  /**
   * Add SDK tracking headers to the given map. These headers are set first so that user-supplied
   * customHeaders can override them.
   */
  private static void addSdkTrackingHeaders(Map<String, String> headers, String module) {
    if (!isSdkHeadersDisabled()) {
      headers.put("x-dashscope-sdk-client", buildSdkClientValue(module));
      headers.put("x-dashscope-sdk-session-id", SDK_SESSION_ID);
    }
  }

  /**
   * SDK-generated tracking headers for the direct OkHttp paths (file upload/download and session
   * SSE). Returns an empty map when {@code DASHSCOPE_DISABLE_SDK_HEADERS} opts out; callers must
   * still keep any headers they set explicitly.
   */
  public static Map<String, String> sdkTrackingHeaders(String module) {
    Map<String, String> headers = new HashMap<>();
    addSdkTrackingHeaders(headers, module);
    return headers;
  }

  // Build HTTP headers with optional custom user agent suffix and module
  public static Map<String, String> buildHttpHeaders(
      String apiKey,
      Boolean isSecurityCheck,
      Protocol protocol,
      Boolean isSSE,
      Boolean isAsyncTask,
      String workspace,
      Map<String, String> customHeaders,
      String customUserAgent,
      String module)
      throws NoApiKeyException {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + ApiKey.getApiKey(apiKey));
    headers.put("user-agent", userAgent(customUserAgent));
    addSdkTrackingHeaders(headers, module);
    if (isSecurityCheck) {
      headers.put("X-DashScope-DataInspection", "enable");
    }
    if (workspace != null && !workspace.isEmpty()) {
      headers.put("X-DashScope-WorkSpace", workspace);
    }
    if (protocol == Protocol.HTTP) {
      if (isAsyncTask) {
        headers.put("X-DashScope-Async", "enable");
      }
      headers.put("Content-Type", "application/json");
      if (isSSE) {
        headers.put("Cache-Control", "no-cache");
        headers.put("Accept", "text/event-stream");
        headers.put("X-Accel-Buffering", "no");
        headers.put("X-DashScope-SSE", "enable");
      } else { // default json response.
        headers.put("Accept", "application/json; charset=utf-8");
      }
    }
    if (!customHeaders.isEmpty()) {
      headers.putAll(customHeaders);
    }
    return headers;
  }
}
