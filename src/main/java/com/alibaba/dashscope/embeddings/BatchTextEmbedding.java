// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.task.AsyncTaskListResult;

public class BatchTextEmbedding {
  private final AsynchronousApi<BatchTextEmbeddingParam> asyncApi;
  private final ApiServiceOption serviceOption;
  private final String baseUrl;

  public final class Models {
    public static final String TEXT_EMBEDDING_ASYNC_V1 = "text-embedding-async-v1";
    public static final String TEXT_EMBEDDING_ASYNC_V2 = "text-embedding-async-v2";
  }

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.DIVIDE)
        .taskGroup(TaskGroup.EMBEDDINGS.getValue())
        .task(Task.TEXT_EMBEDDING.getValue())
        .isAsyncTask(true)
        .function(Function.TEXT_EMBEDDING.getValue())
        .build();
  }

  public BatchTextEmbedding() {
    serviceOption = defaultApiServiceOption();
    asyncApi = new AsynchronousApi<>();
    this.baseUrl = null;
  }

  public BatchTextEmbedding(String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    asyncApi = new AsynchronousApi<>();
    this.baseUrl = baseUrl;
  }

  /**
   * Call the server and wait for the task finished.
   *
   * @param param The input param of class `AsyncTextEmbeddingParam`.
   * @return The output structure of `AsyncTextEmbeddingResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public BatchTextEmbeddingResult call(BatchTextEmbeddingParam param)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(asyncApi.call(param, serviceOption));
  }

  /**
   * @param param The async embedding request parameter, class `AsyncTextEmbeddingParam`
   * @return The async task information of `AsyncTextEmbeddingResult`
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public BatchTextEmbeddingResult asyncCall(BatchTextEmbeddingParam param)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(asyncApi.asyncCall(param, serviceOption));
  }

  public AsyncTaskListResult list(AsyncTaskListParam param) throws ApiException, NoApiKeyException {
    return AsyncTaskListResult.fromDashScopeResult(asyncApi.list(param, baseUrl));
  }

  public AsyncTaskListResult list(
      String startTime,
      String endTime,
      String modelName,
      String apiKeyId,
      String region,
      String status,
      Integer pageNo,
      Integer pageSize)
      throws ApiException, NoApiKeyException {
    return AsyncTaskListResult.fromDashScopeResult(
        asyncApi.list(
            startTime, endTime, modelName, apiKeyId, region, status, pageNo, pageSize, baseUrl));
  }

  public BatchTextEmbeddingResult fetch(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(asyncApi.fetch(taskId, apiKey, baseUrl));
  }

  public BatchTextEmbeddingResult fetch(BatchTextEmbeddingResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(
        asyncApi.fetch(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }

  public BatchTextEmbeddingResult cancel(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(asyncApi.cancel(taskId, apiKey, baseUrl));
  }

  public BatchTextEmbeddingResult cancel(BatchTextEmbeddingResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(
        asyncApi.cancel(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }

  public BatchTextEmbeddingResult wait(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(asyncApi.wait(taskId, apiKey, baseUrl));
  }

  public BatchTextEmbeddingResult wait(BatchTextEmbeddingResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return BatchTextEmbeddingResult.fromDashScopeResult(
        asyncApi.wait(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }
}
