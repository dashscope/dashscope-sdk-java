// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.shared.EnvNames;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.audio.RealtimeParams;
import okhttp3.HttpUrl;

/**
 * {@code audio.realtime} resource (audio design §1.3): OpenAI-Realtime-style sessions on a
 * dedicated socket per session.
 *
 * <p>Endpoint resolution order: explicit {@link RealtimeParams#getUrl()} &gt; {@code
 * DASHSCOPE_WEBSOCKET_OMNI_BASE_URL} (2.x parity) &gt; derived from the resolved HTTP base url.
 * The generic ws base-url override does NOT apply to realtime (audio design §2). Since a session
 * cannot be pooled, prefer connecting slightly ahead of first use to hide the handshake latency.
 */
public final class RealtimeService {
  private static final String REALTIME_PATH = "/api-ws/v1/realtime";

  private final HttpTransport transport;

  public RealtimeService(HttpTransport transport) {
    this.transport = transport;
  }

  /** Connects eagerly; the returned session is ready for {@code updateSession}. */
  public RealtimeSession connect(RealtimeParams params) {
    return connect(params, RequestOptions.NONE);
  }

  public RealtimeSession connect(RealtimeParams params, RequestOptions options) {
    if (params == null) {
      throw new IllegalArgumentException("params must not be null");
    }
    return new RealtimeSession(transport, resolveUrl(params), params, options);
  }

  private String resolveUrl(RealtimeParams params) {
    String explicit = params.getUrl();
    if (explicit == null || explicit.isEmpty()) {
      explicit = System.getenv(EnvNames.DASHSCOPE_WEBSOCKET_OMNI_BASE_URL);
    }
    if (explicit != null && !explicit.isEmpty()) {
      return withModelQuery(explicit, params.getModel());
    }
    return deriveRealtimeUrl(transport.config().getBaseUrl(), params.getModel());
  }

  /** 2.x parity: the model rides on the query string. */
  private static String withModelQuery(String url, String model) {
    if (url.contains("model=")) {
      return url;
    }
    return url + (url.contains("?") ? "&" : "?") + "model=" + model;
  }

  /**
   * Derives the realtime endpoint from the resolved HTTP base url: scheme swap
   * (https→wss/http→ws), path replaced with {@code /api-ws/v1/realtime}, model on the query.
   */
  static String deriveRealtimeUrl(String httpBaseUrl, String model) {
    HttpUrl parsed = HttpUrl.parse(httpBaseUrl);
    if (parsed == null) {
      throw new IllegalArgumentException("invalid base url: " + httpBaseUrl);
    }
    String url =
        parsed.newBuilder().encodedPath(REALTIME_PATH).addQueryParameter("model", model).build()
            .toString();
    if (url.startsWith("https:")) {
      return "wss:" + url.substring("https:".length());
    }
    if (url.startsWith("http:")) {
      return "ws:" + url.substring("http:".length());
    }
    return url;
  }
}
