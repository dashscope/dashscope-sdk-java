// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.PreprocessMessageInput;

public class MultiModalEmbedding {
  private final SynchronizeHalfDuplexApi<MultiModalEmbeddingParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String MULTIMODAL_EMBEDDING_ONE_PEACE_V1 =
        "multimodal-embedding-one-peace-v1";
    public static final String MULTIMODAL_EMBEDDING_V1 = "multimodal-embedding-v1";
  }

  private ApiServiceOption defaulApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.EMBEDDINGS.getValue())
        .task(Task.MULTIMODAL_EMBEDDING.getValue())
        .function(Function.MULTIMODAL_EMBEDDING.getValue())
        .build();
  }

  public MultiModalEmbedding() {
    serviceOption = defaulApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public MultiModalEmbedding(String baseUrl) {
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
   * @throws UploadFileException File upload failed.
   */
  public void call(
      MultiModalEmbeddingParam param, ResultCallback<MultiModalEmbeddingResult> callback)
      throws ApiException, NoApiKeyException, UploadFileException {
    preprocessInput(param);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(MultiModalEmbeddingResult.fromDashScopeResult(message));
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
   * @throws UploadFileException File upload failed.
   */
  public MultiModalEmbeddingResult call(MultiModalEmbeddingParam param)
      throws ApiException, NoApiKeyException, UploadFileException {
    preprocessInput(param);
    return MultiModalEmbeddingResult.fromDashScopeResult(syncApi.call(param));
  }

  private void preprocessInput(MultiModalEmbeddingParam param)
      throws NoApiKeyException, UploadFileException {
    boolean isUpload =
        PreprocessMessageInput.preProcessMessageInputs(
            param.getModel(), param.getContent(), param.getApiKey());
    if (isUpload) {
      param.putHeader("X-DashScope-OssResourceResolve", "enable");
    }
  }
}
