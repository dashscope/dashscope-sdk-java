// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.Version;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiKey;
import java.util.HashMap;
import java.util.Map;

public final class DashScopeHeaders {
  public static String userAgent() {
    String userAgent =
        String.format(
            "dashscope/%s; java/%s; platform/%s; processor/%s",
            Version.version,
            System.getProperty("java.version"),
            System.getProperty("os.name"),
            System.getProperty("os.arch"));
    return userAgent;
  }

  public static Map<String, String> buildWebSocketHeaders(
      String apiKey, boolean isSecurityCheck, String workspace, Map<String, String> customHeaders)
      throws NoApiKeyException {
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + ApiKey.getApiKey(apiKey));
    headers.put("user-agent", userAgent());
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
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + ApiKey.getApiKey(apiKey));
    headers.put("user-agent", userAgent());
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
