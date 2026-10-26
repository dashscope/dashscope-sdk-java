// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.ws.WsConnection;
import com.alibaba.dashscope.types.audio.TtsConnectParams;
import com.alibaba.dashscope.types.audio.TtsSynthesizeParams;
import com.alibaba.dashscope.types.common.ResponseStream;

/**
 * {@code audio.tts} resource: HTTP one-shot/streaming synthesis plus ws duplex sessions. The sugar
 * entries are HTTP forms; duplex-capable models (cosyvoice family) should use {@link #connect}
 * (routing table: {@link AudioModelRouter}).
 */
public final class TtsService {
  private final HttpTransport transport;
  private final TtsHttpService http;

  public TtsService(HttpTransport transport) {
    this.transport = transport;
    this.http = new TtsHttpService(transport);
  }

  /** One-shot synthesis over HTTP SSE (aggregated in memory; prefer stream for long text). */
  public byte[] synthesize(TtsSynthesizeParams params) {
    return http.synthesize(params);
  }

  public byte[] synthesize(TtsSynthesizeParams params, RequestOptions options) {
    return http.synthesize(params, options);
  }

  /** Streaming synthesis over HTTP SSE. */
  public ResponseStream<byte[]> stream(TtsSynthesizeParams params) {
    return http.stream(params);
  }

  public ResponseStream<byte[]> stream(TtsSynthesizeParams params, RequestOptions options) {
    return http.stream(params, options);
  }

  /** Explicit HTTP form (model-name routed); identical to the sugar entries. */
  public TtsHttpService http() {
    return http;
  }

  /** ws duplex session; lazy — nothing connects until {@code start()}. */
  public SynthesisSession connect(TtsConnectParams params) {
    return connect(params, RequestOptions.NONE);
  }

  public SynthesisSession connect(TtsConnectParams params, RequestOptions options) {
    return new SynthesisSession(
        transport, transport.config().getWsBaseUrl(), params, null, options);
  }

  /** ws duplex multiplexed on a shared connection (audio design §7). */
  public SynthesisSession connect(TtsConnectParams params, WsConnection connection) {
    return connect(params, connection, RequestOptions.NONE);
  }

  public SynthesisSession connect(
      TtsConnectParams params, WsConnection connection, RequestOptions options) {
    return new SynthesisSession(
        transport, transport.config().getWsBaseUrl(), params, connection, options);
  }
}
