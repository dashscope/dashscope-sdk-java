// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.Version;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DashScopeHeaders {
  private static final String SDK_CLIENT = "java-sdk";
  private static final String SDK_SESSION_ID = UUID.randomUUID().toString();

  public static String userAgent() {
    return userAgent(null);
  }

  // Generate user agent with optional custom suffix
  public static String userAgent(String customUserAgent) {
    String userAgent =
        StringUtils.format(
            "dashscope/%s; java/%s; platform/%s; processor/%s",
            Version.version,
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
   * Add SDK tracking headers to the given map. These headers are set first so that user-supplied
   * customHeaders can override them.
   */
  private static void addSdkTrackingHeaders(Map<String, String> headers) {
    if (!isSdkHeadersDisabled()) {
      headers.put("x-dashscope-sdk-client", SDK_CLIENT);
      headers.put("x-dashscope-sdk-version", Version.version);
      headers.put("x-dashscope-sdk-session-id", SDK_SESSION_ID);
    }
  }

  public static Map<String, String> buildWebSocketHeaders(
      String apiKey, boolean isSecurityCheck, String workspace, Map<String, String> customHeaders)
      throws NoApiKeyException {
    return buildWebSocketHeaders(apiKey, isSecurityCheck, workspace, customHeaders, null);
  }

  // Build WebSocket headers with optional custom user agent suffix
  public static Map<String, String> buildWebSocketHeaders(
      String apiKey,
      boolean isSecurityCheck,
      String workspace,
      Map<String, String> customHeaders,
      String customUserAgent)
      throws NoApiKeyException {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + ApiKey.getApiKey(apiKey));
    headers.put("user-agent", userAgent(customUserAgent));
    addSdkTrackingHeaders(headers);
    if (workspace != null && !workspace.isEmpty()) {
      headers.put("X-DashScope-WorkSpace", workspace);
    }
    if (isSecurityCheck) {
      headers.put("X-DashScope-DataInspection", "enable");
    }
    if (!customHeaders.isEmpty()) {
      headers.putAll(customHeaders);
    }
    return headers;
  }

  public static Map<String, String> buildHttpHeaders(
      String apiKey,
      Boolean isSecurityCheck,
      Protocol protocol,
      Boolean isSSE,
      Boolean isAsyncTask,
      String workspace,
      Map<String, String> customHeaders)
      throws NoApiKeyException {
    return buildHttpHeaders(
        apiKey, isSecurityCheck, protocol, isSSE, isAsyncTask, workspace, customHeaders, null);
  }

  // Build HTTP headers with optional custom user agent suffix
  public static Map<String, String> buildHttpHeaders(
      String apiKey,
      Boolean isSecurityCheck,
      Protocol protocol,
      Boolean isSSE,
      Boolean isAsyncTask,
      String workspace,
      Map<String, String> customHeaders,
      String customUserAgent)
      throws NoApiKeyException {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + ApiKey.getApiKey(apiKey));
    headers.put("user-agent", userAgent(customUserAgent));
    addSdkTrackingHeaders(headers);
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
