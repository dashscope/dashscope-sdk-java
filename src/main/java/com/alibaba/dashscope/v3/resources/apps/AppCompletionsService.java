// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.apps;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.transport.sse.SseStream;
import com.alibaba.dashscope.v3.types.apps.AppCompletion;
import com.alibaba.dashscope.v3.types.apps.AppCompletionCreateParams;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import java.util.Collections;

/** {@code apps.completions} resource: {@code create} / {@code createStreaming}. */
public final class AppCompletionsService {
  private final HttpTransport transport;

  public AppCompletionsService(HttpTransport transport) {
    this.transport = transport;
  }

  public AppCompletion create(AppCompletionCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public AppCompletion create(AppCompletionCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope =
        transport.execute(
            AppCompletionsSupport.pathOf(params.getAppId()),
            AppCompletionsSupport.buildBody(params),
            AppCompletionsSupport.MODULE,
            options);
    AppCompletion completion = AppCompletionsSupport.mapCompletion(envelope.getBody());
    completion.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return completion;
  }

  /** Streaming create. Always close the stream (try-with-resources); the iterator is
   * single-use. In-stream error frames throw (or surface as a final error frame under
   * {@code raiseOnError(false)}). */
  public ResponseStream<AppCompletion> createStreaming(AppCompletionCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public ResponseStream<AppCompletion> createStreaming(
      AppCompletionCreateParams params, RequestOptions options) {
    SseStream sse =
        transport.executeStream(
            AppCompletionsSupport.pathOf(params.getAppId()),
            AppCompletionsSupport.buildBody(params),
            AppCompletionsSupport.MODULE,
            options);
    return new ResponseStream<>(
        sse,
        frame -> {
          AppCompletion completion = AppCompletionsSupport.mapCompletion(frame);
          completion.fillEnvelope(
              JsonSupport.requestId(frame, null),
              200,
              JsonSupport.errorCode(frame),
              JsonSupport.errorMessage(frame),
              Collections.emptyMap());
          return completion;
        },
        transport.config().isRaiseOnError());
  }
}
