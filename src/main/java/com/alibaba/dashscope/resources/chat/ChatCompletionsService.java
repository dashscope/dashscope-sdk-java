// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.chat;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.sse.SseStream;

/** {@code chat.completions} resource: {@code create} / {@code createStreaming}. */
public final class ChatCompletionsService {
  private final HttpTransport transport;

  public ChatCompletionsService(HttpTransport transport) {
    this.transport = transport;
  }

  public ChatCompletion create(ChatCompletionCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public ChatCompletion create(ChatCompletionCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope =
        transport.execute(
            ChatCompletionsSupport.PATH,
            ChatCompletionsSupport.buildBody(params, false),
            ChatCompletionsSupport.MODULE,
            options);
    ChatCompletion completion = ChatCompletionsSupport.mapCompletion(envelope.getBody());
    completion.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return completion;
  }

  /** Streaming create. Always close the stream (try-with-resources); the iterator is
   * single-use. In-stream error frames throw (or surface as a final error chunk under
   * {@code raiseOnError(false)}). */
  public ResponseStream<ChatCompletionChunk> createStreaming(ChatCompletionCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public ResponseStream<ChatCompletionChunk> createStreaming(
      ChatCompletionCreateParams params, RequestOptions options) {
    SseStream sse =
        transport.executeStream(
            ChatCompletionsSupport.PATH,
            ChatCompletionsSupport.buildBody(params, true),
            ChatCompletionsSupport.MODULE,
            options);
    return new ResponseStream<>(
        sse,
        frame -> {
          ChatCompletionChunk chunk = ChatCompletionsSupport.mapChunk(frame);
          chunk.fillEnvelope(
              JsonSupport.requestId(frame, null),
              200,
              JsonSupport.errorCode(frame),
              JsonSupport.errorMessage(frame),
              java.util.Collections.emptyMap());
          return chunk;
        },
        transport.config().isRaiseOnError());
  }
}
