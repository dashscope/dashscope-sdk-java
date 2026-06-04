// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.imagegeneration;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.utils.OSSUploadCertificate;
import com.alibaba.dashscope.utils.ParamUtils;
import com.alibaba.dashscope.utils.PreprocessMessageInput;
import com.alibaba.dashscope.utils.StreamingMerger;
import com.alibaba.dashscope.utils.StringUtils;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ImageGeneration {
  /* Auto history messages */
  private final SynchronizeHalfDuplexApi<ImageGenerationParam> syncApi;
  private final AsynchronousApi<ImageGenerationParam> asyncApi;
  private final ApiServiceOption serviceOption;
  private final String baseUrl;

  private final ThreadLocal<Map<Integer, AccumulatedData>> accumulatedDataMap =
      ThreadLocal.withInitial(HashMap::new);

  public static class Models {
    public static final String WanX2_6_T2I = "wan2.6-t2i";
    public static final String WanX2_6_IMAGE = "wan2.6-image";
  }

  private ApiServiceOption defaultSyncApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.AIGC.getValue())
        .function(Function.GENERATION.getValue())
        .build();
  }

  /** Creates a copy of the shared serviceOption for thread-safe per-call usage. */
  private ApiServiceOption copyServiceOption() {
    return ApiServiceOption.builder()
        .protocol(serviceOption.getProtocol())
        .httpMethod(serviceOption.getHttpMethod())
        .streamingMode(serviceOption.getStreamingMode())
        .outputMode(serviceOption.getOutputMode())
        .taskGroup(serviceOption.getTaskGroup())
        .task(serviceOption.getTask())
        .function(serviceOption.getFunction())
        .baseHttpUrl(serviceOption.getBaseHttpUrl())
        .baseWebSocketUrl(serviceOption.getBaseWebSocketUrl())
        .build();
  }

  public ImageGeneration() {
    serviceOption = defaultSyncApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    asyncApi = new AsynchronousApi<>();
    this.baseUrl = null;
  }

  public ImageGeneration(String protocol) {
    serviceOption = defaultSyncApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    asyncApi = new AsynchronousApi<>();
    this.baseUrl = null;
  }

  public ImageGeneration(String protocol, String baseUrl) {
    serviceOption = defaultSyncApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    asyncApi = new AsynchronousApi<>();

    this.baseUrl = baseUrl;
  }

  public ImageGeneration(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultSyncApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
    asyncApi = new AsynchronousApi<>();

    this.baseUrl = baseUrl;
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `ImageGenerationParam`.
   * @return The output structure of `ImageGenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws UploadFileException Fail upload failed.
   */
  public ImageGenerationResult call(ImageGenerationParam param)
      throws ApiException, NoApiKeyException, UploadFileException {
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    callOption.setStreamingMode(StreamingMode.NONE);
    callOption.setTask(Task.MULTIMODAL_GENERATION.getValue());
    preprocessInput(param);
    return ImageGenerationResult.fromDashScopeResult(syncApi.call(param, callOption));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `ImageGenerationParam`.
   * @param callback The callback to receive response, the template class is
   *     `ImageGenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws UploadFileException File upload failed.
   */
  public void call(ImageGenerationParam param, ResultCallback<ImageGenerationResult> callback)
      throws ApiException, NoApiKeyException, UploadFileException {
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    callOption.setStreamingMode(StreamingMode.NONE);
    callOption.setTask(Task.MULTIMODAL_GENERATION.getValue());
    preprocessInput(param);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(ImageGenerationResult.fromDashScopeResult(message));
          }

          @Override
          public void onComplete() {
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            callback.onError(e);
          }
        },
        callOption);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `ImageGenerationParam`.
   * @return The output structure of `ImageGenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws UploadFileException Fail upload failed.
   */
  public ImageGenerationResult asyncCall(ImageGenerationParam param)
      throws ApiException, NoApiKeyException, UploadFileException {
    preprocessInput(param);
    ApiServiceOption callOption = copyServiceOption();
    callOption.setTask(Task.IMAGE_GENERATION.getValue());
    callOption.setIsAsyncTask(true);
    return ImageGenerationResult.fromDashScopeResult(asyncApi.asyncCall(param, callOption));
  }

  public ImageGenerationListResult list(AsyncTaskListParam param)
      throws ApiException, NoApiKeyException {
    return ImageGenerationListResult.fromDashScopeResult(asyncApi.list(param, baseUrl));
  }

  public ImageGenerationListResult list(
      String startTime,
      String endTime,
      String modelName,
      String apiKeyId,
      String region,
      String status,
      Integer pageNo,
      Integer pageSize)
      throws ApiException, NoApiKeyException {
    return ImageGenerationListResult.fromDashScopeResult(
        asyncApi.list(
            startTime, endTime, modelName, apiKeyId, region, status, pageNo, pageSize, baseUrl));
  }

  public ImageGenerationResult fetch(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageGenerationResult.fromDashScopeResult(asyncApi.fetch(taskId, apiKey, baseUrl));
  }

  public ImageGenerationResult fetch(ImageGenerationResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {

    return ImageGenerationResult.fromDashScopeResult(
        asyncApi.fetch(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }

  public ImageGenerationResult cancel(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageGenerationResult.fromDashScopeResult(asyncApi.cancel(taskId, apiKey, baseUrl));
  }

  public ImageGenerationResult cancel(ImageGenerationResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    DashScopeResult res = asyncApi.cancel(taskInfo.getOutput().getTaskId(), apiKey, baseUrl);
    return ImageGenerationResult.fromDashScopeResult(res);
  }

  public ImageGenerationResult wait(String taskId, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageGenerationResult.fromDashScopeResult(asyncApi.wait(taskId, apiKey, baseUrl));
  }

  public ImageGenerationResult wait(ImageGenerationResult taskInfo, String apiKey)
      throws ApiException, NoApiKeyException {
    return ImageGenerationResult.fromDashScopeResult(
        asyncApi.wait(taskInfo.getOutput().getTaskId(), apiKey, baseUrl));
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `ImageGenerationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws UploadFileException File upload failed.
   */
  public Flowable<ImageGenerationResult> streamCall(ImageGenerationParam param)
      throws ApiException, NoApiKeyException, UploadFileException {
    // Intercept and modify incrementalOutput parameter if needed
    boolean toMergeResponse = modifyIncrementalOutput(param);
    // Build custom user agent suffix with incremental_to_full flag
    int flagValue = toMergeResponse ? 1 : 0;
    String userAgentSuffix = StringUtils.format("incremental_to_full/%d", flagValue);
    param.putHeader("user-agent", userAgentSuffix);

    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(true);
    callOption.setStreamingMode(StreamingMode.OUT);
    callOption.setTask(Task.MULTIMODAL_GENERATION.getValue());
    preprocessInput(param);
    return syncApi
        .streamCall(param, callOption)
        .map(ImageGenerationResult::fromDashScopeResult)
        .map(result -> mergeSingleResponse(result, toMergeResponse))
        .doFinally(
            () -> {
              if (toMergeResponse) {
                StreamingMerger.clearAccumulatedData(accumulatedDataMap);
              }
            });
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `ImageGenerationParam`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input field is missing.
   * @throws UploadFileException File upload failed.
   */
  public void streamCall(ImageGenerationParam param, ResultCallback<ImageGenerationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException, UploadFileException {
    param.validate();

    // Intercept and modify incrementalOutput parameter if needed
    boolean toMergeResponse = modifyIncrementalOutput(param);

    // Build custom user agent suffix with incremental_to_full flag
    int flagValue = toMergeResponse ? 1 : 0;
    String userAgentSuffix = StringUtils.format("incremental_to_full/%d", flagValue);
    param.putHeader("user-agent", userAgentSuffix);

    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(true);
    callOption.setStreamingMode(StreamingMode.OUT);
    callOption.setTask(Task.MULTIMODAL_GENERATION.getValue());
    preprocessInput(param);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            ImageGenerationResult result = ImageGenerationResult.fromDashScopeResult(msg);
            ImageGenerationResult mergedResult = mergeSingleResponse(result, toMergeResponse);
            if (mergedResult != null) {
              callback.onEvent(mergedResult);
            }
          }

          @Override
          public void onComplete() {
            if (toMergeResponse) {
              StreamingMerger.clearAccumulatedData(accumulatedDataMap);
            }
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            if (toMergeResponse) {
              StreamingMerger.clearAccumulatedData(accumulatedDataMap);
            }
            callback.onError(e);
          }
        },
        callOption);
  }

  private void preprocessInput(ImageGenerationParam param)
      throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    OSSUploadCertificate certificate = null;
    for (ImageGenerationMessage msg : param.getMessages()) {
      if (msg.getRole().equals(Role.USER.getValue())) {
        boolean isUpload;
        PreprocessMessageInput.PreprocessResult result =
            PreprocessMessageInput.preProcessMultiModalMessageInputs(
                param.getModel(), msg,
                param.getApiKey(), certificate);
        isUpload = result.hasUpload();
        certificate = result.getCertificate();
        if (isUpload && !hasUpload) {
          hasUpload = true;
        }
      }
    }
    if (hasUpload) {
      param.putHeader("X-DashScope-OssResourceResolve", "enable");
    }
  }

  /**
   * Modifies the parameters for internal streaming optimization. If incrementalOutput is false,
   * modifies the ImageGenerationParam object to set incrementalOutput to true for internal
   * streaming optimization.
   *
   * @param param The parameter object to modify
   * @return true if the parameter was modified, false otherwise
   */
  private boolean modifyIncrementalOutput(ImageGenerationParam param) {
    Boolean incrementalOutput = param.getIncrementalOutput();
    if (ParamUtils.shouldModifyIncrementalOutput(param.getModel())
        && Boolean.FALSE.equals(incrementalOutput)) {
      // Modify the ImageGenerationParam object to enable incremental output
      param.setIncrementalOutput(true);
      return true;
    }
    return false;
  }

  /**
   * Merges a single ImageGenerationResult with accumulated data for non-incremental output
   * simulation. This method accumulates text content and tool_calls from streaming responses.
   *
   * @param result The ImageGenerationResult to merge
   * @param toMergeResponse Whether to perform merging (based on original incrementalOutput setting)
   * @return The merged ImageGenerationResult
   */
  private ImageGenerationResult mergeSingleResponse(
      ImageGenerationResult result, boolean toMergeResponse) {
    if (!toMergeResponse || result == null || result.getOutput() == null) {
      return result;
    }

    Map<Integer, AccumulatedData> accumulatedData = accumulatedDataMap.get();

    // Handle choices format: output.choices[].message.content
    if (result.getOutput().getChoices() != null) {
      List<ImageGenerationOutput.Choice> choices = result.getOutput().getChoices();
      for (int choiceIdx = 0; choiceIdx < choices.size(); choiceIdx++) {
        ImageGenerationOutput.Choice choice = choices.get(choiceIdx);

        // Initialize accumulated data for this choice if not exists
        AccumulatedData accumulated =
            accumulatedData.computeIfAbsent(choiceIdx, k -> new AccumulatedData());

        if (choice.getMessage() != null) {
          // Handle content accumulation (text content in content list)
          List<Map<String, Object>> currentContent = choice.getMessage().getContent();
          if (currentContent != null && !currentContent.isEmpty()) {
            StreamingMerger.mergeTextContent(currentContent, accumulated.content);
          }
          // Always set the accumulated content if we have any
          if (!accumulated.content.isEmpty()) {
            choice.getMessage().setContent(accumulated.content);
          }
        }
      }
    }

    return result;
  }

  /** Inner class to store accumulated data for response merging. */
  private static class AccumulatedData {
    List<Map<String, Object>> content = new ArrayList<>();
  }
}
