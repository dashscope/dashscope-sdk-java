// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.multimodal;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.oss.OssUploader;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.transport.sse.SseStream;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCompletion;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCreateParams;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code multimodal.chat} resource: {@code create} / {@code createStreaming}. Local-file
 * preprocessing runs before every call (2.x parity); streaming requests for models without
 * server-side cumulative output are merged client-side when the caller asked for {@code
 * incremental_output=false}.
 */
public final class MultimodalChatService {
  private final HttpTransport transport;
  private final OssUploader uploader;

  public MultimodalChatService(HttpTransport transport) {
    this.transport = transport;
    this.uploader = new OssUploader(transport);
  }

  public MultimodalChatCompletion create(MultimodalChatCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public MultimodalChatCompletion create(
      MultimodalChatCreateParams params, RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    MultimodalPreprocessor.ChatResult pre =
        MultimodalPreprocessor.preprocessChat(
            params.getModel(), params.getMessages(), uploader, opts);
    ResponseEnvelope envelope =
        transport.execute(
            MultimodalChatSupport.PATH,
            MultimodalChatSupport.buildBody(withMessages(params, pre.messages), false, false),
            MultimodalChatSupport.MODULE,
            withOssResolve(opts, pre.ossResolve));
    MultimodalChatCompletion completion = MultimodalChatSupport.mapCompletion(envelope.getBody());
    completion.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return completion;
  }

  /**
   * Streaming create. Always close the stream (try-with-resources); the iterator is single-use.
   * When merging applies, every emitted chunk carries cumulative content.
   */
  public ResponseStream<MultimodalChatChunk> createStreaming(MultimodalChatCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public ResponseStream<MultimodalChatChunk> createStreaming(
      MultimodalChatCreateParams params, RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    boolean merge =
        MultimodalChatSupport.shouldModifyIncrementalOutput(params.getModel())
            && Boolean.FALSE.equals(params.getIncrementalOutput());
    MultimodalPreprocessor.ChatResult pre =
        MultimodalPreprocessor.preprocessChat(
            params.getModel(), params.getMessages(), uploader, opts);
    SseStream sse =
        transport.executeStream(
            MultimodalChatSupport.PATH,
            MultimodalChatSupport.buildBody(withMessages(params, pre.messages), true, merge),
            MultimodalChatSupport.MODULE,
            "incremental_to_full/" + (merge ? 1 : 0),
            withOssResolve(opts, pre.ossResolve));
    MultimodalChatSupport.IncrementalMerger merger =
        merge ? new MultimodalChatSupport.IncrementalMerger() : null;
    return new ResponseStream<>(
        sse,
        frame -> {
          ObjectNode flat = MultimodalChatSupport.flatten(frame);
          if (merger != null) {
            flat = merger.merge(flat);
          }
          MultimodalChatChunk chunk = MultimodalChatSupport.mapChunk(flat);
          chunk.fillEnvelope(
              JsonSupport.requestId(frame, null),
              200,
              JsonSupport.errorCode(frame),
              JsonSupport.errorMessage(frame),
              Collections.emptyMap());
          return chunk;
        },
        transport.config().isRaiseOnError());
  }

  private static MultimodalChatCreateParams withMessages(
      MultimodalChatCreateParams params,
      java.util.List<com.alibaba.dashscope.types.multimodal.MultimodalMessage> messages) {
    if (messages == params.getMessages()) {
      return params;
    }
    return MultimodalChatCreateParams.builder()
        .model(params.getModel())
        .messages(messages)
        .temperature(params.getTemperature())
        .topP(params.getTopP())
        .topK(params.getTopK())
        .maxTokens(params.getMaxTokens())
        .seed(params.getSeed())
        .stop(params.getStop())
        .repetitionPenalty(params.getRepetitionPenalty())
        .enableThinking(params.getEnableThinking())
        .thinkingBudget(params.getThinkingBudget())
        .responseFormat(params.getResponseFormat())
        .toolChoice(params.getToolChoice())
        .tools(params.getTools())
        .n(params.getN())
        .enableSearch(params.getEnableSearch())
        .searchOptions(params.getSearchOptions())
        .incrementalOutput(params.getIncrementalOutput())
        .vlHighResolutionImages(params.getVlHighResolutionImages())
        .parameters(params.getParameters())
        .extraInput(params.getExtraInput())
        .build();
  }

  private static RequestOptions withOssResolve(RequestOptions options, boolean ossResolve) {
    if (!ossResolve) {
      return options;
    }
    Map<String, String> headers = new LinkedHashMap<>(options.getHeaders());
    headers.put(MultimodalPreprocessor.OSS_RESOLVE_HEADER, "enable");
    return RequestOptions.builder()
        .timeout(options.getTimeout())
        .maxRetries(options.getMaxRetries())
        .apiKey(options.getApiKey())
        .workspace(options.getWorkspace())
        .headers(headers)
        .build();
  }
}
