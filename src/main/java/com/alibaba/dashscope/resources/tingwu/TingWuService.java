// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.tingwu;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.alibaba.dashscope.types.tingwu.TingWuRealtimeParams;

/**
 * {@code tingwu} resource: offline summarization as a long task ({@link #jobs()}) plus realtime
 * understanding over a ws duplex session ({@link #realtime}).
 */
public final class TingWuService {
  static final String PATH = "/services/aigc/multimodal-generation/generation";
  static final String MODULE = "multimodal";

  private final HttpTransport transport;
  private final TingWuJobsService jobs;

  public TingWuService(HttpTransport transport) {
    this.transport = transport;
    this.jobs = new TingWuJobsService(transport);
  }

  /** The offline-task handle: create/fetch/wait/cancel/list. */
  public TingWuJobsService jobs() {
    return jobs;
  }

  /** Realtime understanding on a dedicated socket; lazy — nothing connects until {@code start()}. */
  public TingWuRealtimeSession realtime(TingWuRealtimeParams params) {
    return realtime(params, RequestOptions.NONE);
  }

  public TingWuRealtimeSession realtime(TingWuRealtimeParams params, RequestOptions options) {
    return new TingWuRealtimeSession(
        transport, transport.config().getWsBaseUrl(), params, null, options);
  }

  /**
   * Realtime understanding multiplexed on a shared connection. {@code close()} on the returned
   * session detaches the task only; the socket stays open.
   */
  public TingWuRealtimeSession realtime(TingWuRealtimeParams params, WsConnection connection) {
    return realtime(params, connection, RequestOptions.NONE);
  }

  public TingWuRealtimeSession realtime(
      TingWuRealtimeParams params, WsConnection connection, RequestOptions options) {
    return new TingWuRealtimeSession(
        transport, transport.config().getWsBaseUrl(), params, connection, options);
  }
}
