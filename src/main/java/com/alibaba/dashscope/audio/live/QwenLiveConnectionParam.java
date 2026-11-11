// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.live;

import static com.alibaba.dashscope.utils.Constants.DASHSCOPE_WEBSOCKET_QWEN_LIVE_BASE_URL_ENV;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import okhttp3.HttpUrl;

@Data
@SuperBuilder
public class QwenLiveConnectionParam {

  public static String baseWebsocketApiUrlTemplate =
      System.getenv()
          .getOrDefault(
              DASHSCOPE_WEBSOCKET_QWEN_LIVE_BASE_URL_ENV,
              "wss://{workspaceId}.cn-beijing.maas.aliyuncs.com/api-ws/v1/realtime");

  private static final String PLACEHOLDER_LOWER_CAMEL = "{workspaceId}";

  private static final String PLACEHOLDER_UPPER_CAMEL = "{WorkspaceId}";

  @Getter @lombok.NonNull private String model;

  @Getter private String apikey;

  @Getter private String workspaceId;

  @Getter private String workspace;

  @Singular protected Map<String, Object> headers;

  private String url;

  private Executor callbackExecutor;

  @Builder.Default private int callbackQueueCapacity = 256;

  public Map<String, String> getHeaders() {
    Map<String, String> res = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      res.put(entry.getKey(), entry.getValue().toString());
    }
    return res;
  }

  public String getUrl() {
    if (isBlank(model)) {
      throw new IllegalArgumentException("model must not be blank.");
    }
    String candidate = url != null ? url : baseWebsocketApiUrlTemplate;
    if (isBlank(candidate)) {
      throw new IllegalArgumentException("Qwen Live websocket url is not configured.");
    }
    return appendModelQuery(resolveWorkspacePlaceholder(candidate));
  }

  private String resolveWorkspacePlaceholder(String candidate) {
    boolean hasPlaceholder =
        candidate.contains(PLACEHOLDER_LOWER_CAMEL) || candidate.contains(PLACEHOLDER_UPPER_CAMEL);
    if (!hasPlaceholder) {
      return candidate;
    }
    if (isBlank(workspaceId)) {
      throw new IllegalArgumentException(
          "workspaceId is required by the websocket url template: " + candidate);
    }
    return candidate
        .replace(PLACEHOLDER_LOWER_CAMEL, workspaceId)
        .replace(PLACEHOLDER_UPPER_CAMEL, workspaceId);
  }

  // HttpUrl only parses http/https: map ws/wss temporarily, set the model query, then restore.
  private String appendModelQuery(String rawUrl) {
    String originScheme = null;
    String parsableScheme = null;
    String parsable = rawUrl;
    String lower = rawUrl.toLowerCase(Locale.ROOT);
    for (String scheme : new String[] {"wss", "ws", "https", "http"}) {
      if (lower.startsWith(scheme + "://")) {
        originScheme = scheme;
        parsableScheme = "wss".equals(scheme) || "https".equals(scheme) ? "https" : "http";
        parsable = parsableScheme + rawUrl.substring(scheme.length());
        break;
      }
    }
    HttpUrl parsed = HttpUrl.parse(parsable);
    if (parsed == null) {
      throw new IllegalArgumentException("Invalid websocket url: " + rawUrl);
    }
    String built = parsed.newBuilder().setQueryParameter("model", model).build().toString();
    return originScheme == null ? built : originScheme + built.substring(parsableScheme.length());
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
