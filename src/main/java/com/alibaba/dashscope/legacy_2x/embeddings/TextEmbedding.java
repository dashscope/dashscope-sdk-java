// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.embeddings;

import com.alibaba.dashscope.legacy_2x.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.common.Function;
import com.alibaba.dashscope.legacy_2x.common.OutputMode;
import com.alibaba.dashscope.legacy_2x.common.ResultCallback;
import com.alibaba.dashscope.legacy_2x.common.Task;
import com.alibaba.dashscope.legacy_2x.common.TaskGroup;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.protocol.ApiServiceOption;
import com.alibaba.dashscope.legacy_2x.protocol.HttpMethod;
import com.alibaba.dashscope.legacy_2x.protocol.Protocol;
import com.alibaba.dashscope.legacy_2x.protocol.StreamingMode;

public class TextEmbedding {
  private final SynchronizeHalfDuplexApi<TextEmbeddingParam> syncApi;
  private final ApiServiceOption serviceOption;

  public final class Models {
    public static final String TEXT_EMBEDDING_V1 = "text-embedding-v1";
    public static final String TEXT_EMBEDDING_V2 = "text-embedding-v2";
    public static final String TEXT_EMBEDDING_V3 = "text-embedding-v3";
    public static final String TEXT_EMBEDDING_V4 = "text-embedding-v4";
  }

  private ApiServiceOption defaulApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.DIVIDE)
        .taskGroup(TaskGroup.EMBEDDINGS.getValue())
        .task(Task.TEXT_EMBEDDING.getValue())
        .function(Function.TEXT_EMBEDDING.getValue())
        .build();
  }

  public TextEmbedding() {
    serviceOption = defaulApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextEmbedding(String baseUrl) {
    serviceOption = defaulApiServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `GenerationParam`.
   * @param callback The callback to receive response, the template class is `GenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public void call(TextEmbeddingParam param, ResultCallback<TextEmbeddingResult> callback)
      throws ApiException, NoApiKeyException {
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(TextEmbeddingResult.fromDashScopeResult(message));
          }

          @Override
          public void onComplete() {
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            callback.onError(e);
          }
        });
  }

  /**
   * Call the server to get the whole result, only http protocol
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public TextEmbeddingResult call(TextEmbeddingParam param) throws ApiException, NoApiKeyException {
    return TextEmbeddingResult.fromDashScopeResult(syncApi.call(param));
  }
}
