// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.MultiModalMessage;
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
import com.alibaba.dashscope.tools.ToolCallBase;
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
public final class MultiModalConversation {
  /* Auto history messages */
  private final SynchronizeHalfDuplexApi<MultiModalConversationParam> syncApi;
  private final ApiServiceOption serviceOption;


  public static class Models {
    public static final String QWEN_VL_CHAT_V1 = "qwen-vl-chat-v1";
    public static final String QWEN_VL_PLUS = "qwen-vl-plus";
  }

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.AIGC.getValue())
        .task(Task.MULTIMODAL_GENERATION.getValue())
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

  public MultiModalConversation() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public MultiModalConversation(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public MultiModalConversation(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public MultiModalConversation(
      String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }
  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @return The output structure of `MultiModalConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws UploadFileException Fail upload failed.
   */
  public MultiModalConversationResult call(MultiModalConversationParam param)
      throws ApiException, NoApiKeyException, UploadFileException {
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    callOption.setStreamingMode(StreamingMode.NONE);
    preprocessInput(param);
    return MultiModalConversationResult.fromDashScopeResult(syncApi.call(param, callOption));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @param callback The callback to receive response, the template class is
   *     `MultiModalConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws UploadFileException File upload failed.
   */
  public void call(
      MultiModalConversationParam param, ResultCallback<MultiModalConversationResult> callback)
      throws ApiException, NoApiKeyException, UploadFileException {
    ApiServiceOption callOption = copyServiceOption();
    callOption.setIsSSE(false);
    callOption.setStreamingMode(StreamingMode.NONE);
    preprocessInput(param);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(MultiModalConversationResult.fromDashScopeResult(message));
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
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws UploadFileException File upload failed.
   */
  public Flowable<MultiModalConversationResult> streamCall(MultiModalConversationParam param)
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
    preprocessInput(param);
    return Flowable.defer(
        () -> {
          Map<Integer, AccumulatedData> accumulatedData = new HashMap<>();
          return syncApi
              .streamCall(param, callOption)
              .map(MultiModalConversationResult::fromDashScopeResult)
              .map(result -> mergeSingleResponse(result, toMergeResponse, accumulatedData))
              .doFinally(accumulatedData::clear);
        });
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `MultiModalConversationParam`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input field is missing.
   * @throws UploadFileException File upload failed.
   */
  public void streamCall(
      MultiModalConversationParam param, ResultCallback<MultiModalConversationResult> callback)
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
    preprocessInput(param);
    Map<Integer, AccumulatedData> accumulatedData = new HashMap<>();
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            MultiModalConversationResult result =
                MultiModalConversationResult.fromDashScopeResult(msg);
            MultiModalConversationResult mergedResult =
                mergeSingleResponse(result, toMergeResponse, accumulatedData);
            if (mergedResult != null) {
              callback.onEvent(mergedResult);
            }
          }

          @Override
          public void onComplete() {
            accumulatedData.clear();
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            accumulatedData.clear();
            callback.onError(e);
          }
        },
        callOption);
  }

  private void preprocessInput(MultiModalConversationParam param)
      throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    OSSUploadCertificate certificate = null;

    for (Object msg : param.getMessages()) {
      boolean isUpload = false;
      if (msg instanceof MultiModalConversationMessage) {
        MultiModalConversationMessage realMsg = (MultiModalConversationMessage) msg;
        if (realMsg.getRole().equals(Role.USER.getValue())) {
          PreprocessMessageInput.PreprocessResult result =
              PreprocessMessageInput.preProcessMessageInputs(
                  param.getModel(),
                  ((MultiModalConversationMessage) msg).getContent(),
                  param.getApiKey(),
                  certificate);
          isUpload = result.hasUpload();
          certificate = result.getCertificate();
        }
      } else {
        MultiModalMessage realMsg = (MultiModalMessage) msg;
        if (realMsg.getRole().equals(Role.USER.getValue())) {
          PreprocessMessageInput.PreprocessResult result =
              PreprocessMessageInput.preProcessMultiModalMessageInputs(
                  param.getModel(), (MultiModalMessage) msg, param.getApiKey(), certificate);
          isUpload = result.hasUpload();
          certificate = result.getCertificate();
        }
      }
      if (isUpload && !hasUpload) {
        hasUpload = true;
      }
    }
    if (hasUpload) {
      param.putHeader("X-DashScope-OssResourceResolve", "enable");
    }
  }

  /**
   * Modifies the parameters for internal streaming optimization. If incrementalOutput is false,
   * modifies the MultiModalConversationParam object to set incrementalOutput to true for internal
   * streaming optimization.
   *
   * @param param The parameter object to modify
   * @return true if the parameter was modified, false otherwise
   */
  private boolean modifyIncrementalOutput(MultiModalConversationParam param) {
    Boolean incrementalOutput = param.getIncrementalOutput();
    if (ParamUtils.shouldModifyIncrementalOutput(param.getModel())
        && Boolean.FALSE.equals(incrementalOutput)) {
      // Modify the MultiModalConversationParam object to enable incremental output
      param.setIncrementalOutput(true);
      return true;
    }
    return false;
  }

  /**
   * Merges a single MultiModalConversationResult with accumulated data for non-incremental output
   * simulation. This method accumulates text content and tool_calls from streaming responses.
   *
   * @param result The MultiModalConversationResult to merge
   * @param toMergeResponse Whether to perform merging (based on original incrementalOutput setting)
   * @param accumulatedData The per-stream accumulated data
   * @return The merged MultiModalConversationResult
   */
  private MultiModalConversationResult mergeSingleResponse(
      MultiModalConversationResult result,
      boolean toMergeResponse,
      Map<Integer, AccumulatedData> accumulatedData) {
    if (!toMergeResponse || result == null || result.getOutput() == null) {
      return result;
    }

    // Handle choices format: output.choices[].message.content
    if (result.getOutput().getChoices() != null) {
      List<MultiModalConversationOutput.Choice> choices = result.getOutput().getChoices();
      for (int choiceIdx = 0; choiceIdx < choices.size(); choiceIdx++) {
        MultiModalConversationOutput.Choice choice = choices.get(choiceIdx);

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

          // Handle reasoning_content accumulation
          String currentReasoningContent = choice.getMessage().getReasoningContent();
          if (currentReasoningContent != null && !currentReasoningContent.isEmpty()) {
            accumulated.reasoningContent.append(currentReasoningContent);
          }
          // Always set the accumulated reasoning_content if we have any
          if (accumulated.reasoningContent.length() > 0) {
            choice.getMessage().setReasoningContent(accumulated.reasoningContent.toString());
          }

          // Handle tool_calls accumulation (delegate to shared utility)
          List<ToolCallBase> currentToolCalls = choice.getMessage().getToolCalls();
          if (currentToolCalls != null && !currentToolCalls.isEmpty()) {
            StreamingMerger.mergeToolCalls(currentToolCalls, accumulated.toolCalls);
          }
          // Always set accumulated tool_calls if we have any
          if (!accumulated.toolCalls.isEmpty()) {
            choice.getMessage().setToolCalls(accumulated.toolCalls);
          }
        }
      }
    }

    return result;
  }

  /** Inner class to store accumulated data for response merging. */
  private static class AccumulatedData {
    List<Map<String, Object>> content = new ArrayList<>();
    List<ToolCallBase> toolCalls = new ArrayList<>();
    StringBuilder reasoningContent = new StringBuilder();
  }
}
