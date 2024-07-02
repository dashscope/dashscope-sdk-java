// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.api;

import static com.alibaba.dashscope.utils.ApiKeywords.TASK_STATUS;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.TaskStatus;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.task.AsyncTaskParam;
import com.google.gson.JsonObject;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/** Support DashScope async task CRUD. */
public final class AsynchronousApi<ParamT extends HalfDuplexParamBase> {
  final HalfDuplexClient client;
  ConnectionOptions connectionOptions;

  /** Create default http client. */
  public AsynchronousApi() {
    this.client = ClientProviders.getHalfDuplexClient("https");
    this.connectionOptions = null;
  }

  /**
   * Create custom http client
   *
   * @param connectionOptions The client option.
   */
  public AsynchronousApi(ConnectionOptions connectionOptions) {
    this.client = ClientProviders.getHalfDuplexClient(connectionOptions, "https");
    this.connectionOptions = connectionOptions;
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param, should be the subclass of `Param`.
   * @param serviceOption The service option.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @return The output structure, should be the subclass of `Result`.
   */
  public DashScopeResult call(ParamT param, ServiceOption serviceOption)
      throws ApiException, NoApiKeyException {
    DashScopeResult task = this.asyncCall(param, serviceOption);
    return this.wait(getTaskId(task), param.getApiKey(), serviceOption.getBaseHttpUrl());
  }

  /**
   * Call async interface and return async task info.
   *
   * @param param The input param, should be the subclass of `Param`
   * @param serviceOption The service option.
   * @return The output task information, should be the subclass of `Result`
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult asyncCall(ParamT param, ServiceOption serviceOption)
      throws ApiException, NoApiKeyException {
    HalfDuplexRequest req = new HalfDuplexRequest(param, serviceOption);
    return client.send(req);
  }

  /**
   * Wait for async task completed and return task result.
   *
   * @param taskId The async task id.
   * @param apiKey The api-key.
   * @param baseUrl The base http url.
   * @param customHeaders The custom headers.
   * @return The task result.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult wait(
      String taskId, String apiKey, String baseUrl, Map<String, String> customHeaders)
      throws ApiException, NoApiKeyException {
    AsyncTaskOption serviceOption =
        AsyncTaskOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .url(String.format("/tasks/%s", taskId))
            .baseHttpUrl(baseUrl)
            .build();

    AsyncTaskParam getParam =
        AsyncTaskParam.builder().taskId(taskId).apiKey(apiKey).headers(customHeaders).build();
    HalfDuplexRequest req = new HalfDuplexRequest(getParam, serviceOption);
    int waitMilliseconds = 1000;
    int maxWaitMilliseconds = 5 * 1000;
    int incrementSteps = 3;
    int step = 0;
    while (true) {
      try {
        DashScopeResult taskResult = client.send(req);
        JsonObject output = (JsonObject) taskResult.getOutput();
        String taskStatus =
            output.get(TASK_STATUS) == null ? null : output.get(TASK_STATUS).getAsString();
        if (TaskStatus.FAILED.getValue().equals(taskStatus)
            || TaskStatus.CANCELED.getValue().equals(taskStatus)
            || TaskStatus.UNKNOWN.getValue().equals(taskStatus)) {
          return taskResult;
        } else if (TaskStatus.SUCCEEDED.getValue().equals(taskStatus)) {
          return taskResult;
        } else {
          // we start by querying once every second, and double the query interval after
          // every 3(increment_steps) intervals, until we hit the max waiting interval of 5(seconds）
          // TODO: investigate if we can use long-poll (server side return immediately when ready)
          step += 1;
          if (waitMilliseconds < maxWaitMilliseconds && step % incrementSteps == 0) {
            waitMilliseconds =
                waitMilliseconds * 2 > maxWaitMilliseconds
                    ? maxWaitMilliseconds
                    : waitMilliseconds * 2;
          }
          try {
            Thread.sleep(waitMilliseconds);
          } catch (InterruptedException ignored) {
          }
        }
      } catch (ApiException e) {
        if (e.getStatus().getStatusCode() != HttpURLConnection.HTTP_UNAVAILABLE
            && e.getStatus().getStatusCode() != HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
          throw e;
        }
      }
    }
  }

  /**
   * Wait for async task completed and return task result.
   *
   * @param taskId The async task id.
   * @param apiKey The api-key.
   * @return The task result.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult wait(String taskId, String apiKey, String baseUrl)
      throws ApiException, NoApiKeyException {
    return wait(taskId, apiKey, baseUrl, new HashMap<>());
  }

  public DashScopeResult wait(DashScopeResult taskInfo, String apiKey, String baseUrl)
      throws ApiException, NoApiKeyException {
    return wait(getTaskId(taskInfo), apiKey, baseUrl);
  }

  /**
   * Get the async task information, if the is completed will return the result, otherwise return
   * task status.
   *
   * @param taskId The async task id.
   * @param apiKey The api key.
   * @param baseUrl The base http url.
   * @param customHeaders The custom headers.
   * @return The task result or status information.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult fetch(
      String taskId, String apiKey, String baseUrl, Map<String, String> customHeaders)
      throws ApiException, NoApiKeyException {
    AsyncTaskOption serviceOption =
        AsyncTaskOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .baseHttpUrl(baseUrl)
            .url(String.format("/tasks/%s", taskId))
            .build();

    AsyncTaskParam getParam =
        AsyncTaskParam.builder().taskId(taskId).apiKey(apiKey).headers(customHeaders).build();
    HalfDuplexRequest req = new HalfDuplexRequest(getParam, serviceOption);
    DashScopeResult taskResult = client.send(req);
    return taskResult;
  }

  /**
   * Get the async task information, if the is completed will return the result, otherwise return
   * task status.
   *
   * @param taskId The async task id.
   * @param apiKey The api key.
   * @return The task result or status information.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public DashScopeResult fetch(String taskId, String apiKey, String baseUrl)
      throws ApiException, NoApiKeyException {
    return fetch(taskId, apiKey, baseUrl, new HashMap<>());
  }

  public DashScopeResult fetch(DashScopeResult taskInfo, String apiKey, String baseUrl)
      throws ApiException, NoApiKeyException {
    return fetch(getTaskId(taskInfo), apiKey, baseUrl);
  }

  public DashScopeResult cancel(String taskId, String apiKey, String baseUrl)
      throws ApiException, NoApiKeyException {
    AsyncTaskParam param = AsyncTaskParam.builder().taskId(taskId).apiKey(apiKey).build();
    AsyncTaskOption taskOption =
        AsyncTaskOption.builder()
            .baseHttpUrl(baseUrl)
            .url(String.format("/tasks/%s/cancel", taskId))
            .build();
    DashScopeResult result = client.send(new HalfDuplexRequest(param, taskOption));
    return result;
  }

  public DashScopeResult cancel(DashScopeResult taskInfo, String apiKey, String baseUrl)
      throws ApiException, NoApiKeyException {
    return cancel(getTaskId(taskInfo), apiKey, baseUrl);
  }

  public DashScopeResult list(AsyncTaskListParam param, String baseUrl)
      throws ApiException, NoApiKeyException {
    AsyncTaskOption taskOption =
        AsyncTaskOption.builder()
            .baseHttpUrl(baseUrl)
            .url("/tasks")
            .httpMethod(HttpMethod.GET)
            .build();
    DashScopeResult result = client.send(new HalfDuplexRequest(param, taskOption));
    return result;
  }

  public DashScopeResult list(
      String startTime,
      String endTime,
      String modelName,
      String apiKeyId,
      String region,
      String status,
      Integer pageNo,
      Integer pageSize,
      String baseUrl)
      throws ApiException, NoApiKeyException {
    AsyncTaskListParam.AsyncTaskListParamBuilder<?, ?> builder = AsyncTaskListParam.builder();
    if (startTime != null) {
      builder.parameter("start_time", startTime);
    }
    if (endTime != null) {
      builder.parameter("end_time", endTime);
    }
    if (modelName != null) {
      builder.parameter("model_name", modelName);
    }
    if (apiKeyId != null) {
      builder.parameter("api_key_id", apiKeyId);
    }
    if (region != null) {
      builder.parameter("region", region);
    }
    if (status != null) {
      builder.parameter("status", status);
    }
    if (pageNo != null) {
      builder.parameter("page_no", pageNo);
    }
    if (pageSize != null) {
      builder.parameter("page_size", pageSize);
    }
    AsyncTaskListParam param = builder.build();
    return list(param, baseUrl);
  }

  public String getTaskId(DashScopeResult task) {
    JsonObject output = (JsonObject) (task.getOutput());
    return output.get("task_id").getAsString();
  }
}
