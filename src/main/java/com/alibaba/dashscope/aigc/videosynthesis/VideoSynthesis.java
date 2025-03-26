// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.videosynthesis;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.task.AsyncTaskListParam;

public final class VideoSynthesis {
  /** default task, function & taskGroup */
  private final String task = "video-generation";

  private final String function = "video-synthesis";
  private final String taskGroup = "aigc";

  private final AsynchronousApi<HalfDuplexServiceParam> asyncApi;
  private final ApiServiceOption createServiceOptions;
  private final String baseUrl;

  // Model name
  public static class Models {
    @Deprecated public static final String WANX_TXT_TO_VIDEO_PRO = "wanx-txt2video-pro";
    @Deprecated public static final String WANX_IMG_TO_VIDEO_PRO = "wanx-img2video-pro";

    public static final String WANX_2_1_T2V_PLUS = "wanx2.1-t2v-plus";
    public static final String WANX_2_1_T2V_TURBO = "wanx2.1-t2v-turbo";

    public static final String WANX_2_1_I2V_PLUS = "wanx2.1-i2v-plus";
    public static final String WANX_2_1_I2V_TURBO = "wanx2.1-i2v-turbo";
  }

  /** Video synthesis size */
  public static class Size {
    public static final String DEFAULT = "1280*720";
  }

  /** Video synthesis duration */
  public static class Duration {
    public static final int DEFAULT = 5;
  }

  /**
   * Create ApiServiceOption
   *
   * @return ApiServiceOption
   */
  private ApiServiceOption getApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .taskGroup(taskGroup)
        .task(task)
        .function(function)
        .isAsyncTask(true)
        .build();
  }

  /** default VideoSynthesis constructor */
  public VideoSynthesis() {
    // only support http
    asyncApi = new AsynchronousApi<>();
    createServiceOptions = getApiServiceOption();
    this.baseUrl = null;
  }

  /**
   * Create with custom baseUrl
   *
   * @param baseUrl The service base url.
   */
  public VideoSynthesis(String baseUrl) {
    // only support http
    asyncApi = new AsynchronousApi<>();
    createServiceOptions = getApiServiceOption();
    this.baseUrl = baseUrl;
  }

  /**
   * Async call
   *
   * @param param The input param of class VideoSynthesisParam
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Check the input param.
   */
  public VideoSynthesisResult asyncCall(VideoSynthesisParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    return VideoSynthesisResult.fromDashScopeResult(
        asyncApi.asyncCall(param, createServiceOptions));
  }

  /**
   * Call the server to get the result.
   *
   * @param param The input param of class `VideoSynthesisParam`.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Check the input param.
   */
  public VideoSynthesisResult call(VideoSynthesisParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    return VideoSynthesisResult.fromDashScopeResult(asyncApi.call(param, createServiceOptions));
  }

  /**
   * List the tasks.
   *
   * @param param The input param of class `AsyncTaskListParam`.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisListResult list(AsyncTaskListParam param)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisListResult.fromDashScopeResult(asyncApi.list(param, baseUrl));
  }

  /**
   * @param startTime startTime
   * @param endTime endTime
   * @param modelName modelName
   * @param apiKeyId apiKeyId
   * @param region region
   * @param status status
   * @param pageNo pageNo
   * @param pageSize pageSize
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisListResult list(
      String startTime,
      String endTime,
      String modelName,
      String apiKeyId,
      String region,
      String status,
      Integer pageNo,
      Integer pageSize)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisListResult.fromDashScopeResult(
        asyncApi.list(
            startTime, endTime, modelName, apiKeyId, region, status, pageNo, pageSize, baseUrl));
  }

  /**
   * Fetch the result.
   *
   * @param taskId The task id.
   * @param apiKey The api key.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisResult fetch(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisResult.fromDashScopeResult(asyncApi.fetch(taskId, apiKey, baseUrl));
  }

  /**
   * Fetch the result.
   *
   * @param taskInfo The task info.
   * @param apiKey The api key.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisResult fetch(VideoSynthesisResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisResult.fromDashScopeResult(
        asyncApi.fetch(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }

  /**
   * Cancel the task.
   *
   * @param taskId The task id.
   * @param apiKey The api key.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisResult cancel(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisResult.fromDashScopeResult(asyncApi.cancel(taskId, apiKey, baseUrl));
  }

  /**
   * Cancel the task.
   *
   * @param taskInfo The task info.
   * @param apiKey The api key.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisResult cancel(VideoSynthesisResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    DashScopeResult res = asyncApi.cancel(taskInfo.getOutput().getTaskId(), apiKey, baseUrl);
    return VideoSynthesisResult.fromDashScopeResult(res);
  }

  /**
   * Wait for the task to complete.
   *
   * @param taskId The task id.
   * @param apiKey The api key.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisResult wait(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisResult.fromDashScopeResult(asyncApi.wait(taskId, apiKey, baseUrl));
  }

  /**
   * Wait for the task to complete.
   *
   * @param taskInfo The task info.
   * @param apiKey The api key.
   * @return The video synthesis result.
   * @throws NoApiKeyException Can not find api key.
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public VideoSynthesisResult wait(VideoSynthesisResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return VideoSynthesisResult.fromDashScopeResult(
        asyncApi.wait(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }
}
