// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.protocol.*;
import com.alibaba.dashscope.utils.ParamUtils;
import com.alibaba.dashscope.utils.PreprocessMessageInput;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;

@Slf4j
public final class MultiModalConversation {
  /* Auto history messages */
  private final SynchronizeHalfDuplexApi<MultiModalConversationParam> syncApi;
  private final ApiServiceOption serviceOption;

  private final ThreadLocal<Map<Integer, AccumulatedData>> accumulatedDataMap =
      ThreadLocal.withInitial(HashMap::new);

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
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    preprocessInput(param);
    return MultiModalConversationResult.fromDashScopeResult(syncApi.call(param));
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
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
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
        });
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
    String userAgentSuffix = String.format("incremental_to_full/%d", flagValue);
    param.putHeader("user-agent", userAgentSuffix);

    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    preprocessInput(param);
    return syncApi
        .streamCall(param)
        .map(MultiModalConversationResult::fromDashScopeResult)
        .map(result -> mergeSingleResponse(result, toMergeResponse))
        .doOnComplete(() -> {
          if (toMergeResponse) {
            clearAccumulatedData();
          }
        })
        .doOnError(throwable -> {
          if (toMergeResponse) {
            clearAccumulatedData();
          }
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
    String userAgentSuffix = String.format("incremental_to_full/%d", flagValue);
    param.putHeader("user-agent", userAgentSuffix);

    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    preprocessInput(param);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            MultiModalConversationResult result = MultiModalConversationResult.fromDashScopeResult(msg);
            MultiModalConversationResult mergedResult = mergeSingleResponse(result, toMergeResponse);
            callback.onEvent(mergedResult);
          }

          @Override
          public void onComplete() {
            if (toMergeResponse) {
              clearAccumulatedData();
            }
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            if (toMergeResponse) {
              clearAccumulatedData();
            }
            callback.onError(e);
          }
        });
  }

  private void preprocessInput(MultiModalConversationParam param)
      throws NoApiKeyException, UploadFileException {
    boolean hasUpload = false;
    for (Object msg : param.getMessages()) {
      boolean isUpload = false;
      if (msg instanceof MultiModalConversationMessage) {
        isUpload =
            PreprocessMessageInput.preProcessMessageInputs(
                param.getModel(),
                ((MultiModalConversationMessage) msg).getContent(),
                param.getApiKey());

      } else {
        isUpload =
            PreprocessMessageInput.preProcessMultiModalMessageInputs(
                param.getModel(), (MultiModalMessage) msg, param.getApiKey());
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
   * Modifies the parameters for internal streaming optimization.
   * If incrementalOutput is false, modifies the MultiModalConversationParam object to set
   * incrementalOutput to true for internal streaming optimization.
   *
   * @param param The parameter object to modify
   * @return true if the parameter was modified, false otherwise
   */
  private boolean modifyIncrementalOutput(MultiModalConversationParam param) {
    Boolean incrementalOutput = param.getIncrementalOutput();
    if (ParamUtils.shouldModifyIncrementalOutput(param.getModel()) &&
            Boolean.FALSE.equals(incrementalOutput)) {
      // Modify the MultiModalConversationParam object to enable incremental output
      param.setIncrementalOutput(true);
      return true;
    }
    return false;
  }

  /**
   * Merges a single MultiModalConversationResult with accumulated data for non-incremental output simulation.
   * This method accumulates text content and tool_calls from streaming responses.
   *
   * @param result The MultiModalConversationResult to merge
   * @param toMergeResponse Whether to perform merging (based on original incrementalOutput setting)
   * @return The merged MultiModalConversationResult
   */
  private MultiModalConversationResult mergeSingleResponse(MultiModalConversationResult result, boolean toMergeResponse) {
    if (!toMergeResponse || result == null || result.getOutput() == null) {
      return result;
    }

    Map<Integer, AccumulatedData> accumulatedData = accumulatedDataMap.get();

    // Handle choices format: output.choices[].message.content
    if (result.getOutput().getChoices() != null) {
      List<MultiModalConversationOutput.Choice> choices = result.getOutput().getChoices();
      for (int choiceIdx = 0; choiceIdx < choices.size(); choiceIdx++) {
        MultiModalConversationOutput.Choice choice = choices.get(choiceIdx);

        // Initialize accumulated data for this choice if not exists
        AccumulatedData accumulated = accumulatedData.computeIfAbsent(
                choiceIdx, k -> new AccumulatedData());

        if (choice.getMessage() != null) {
          // Handle content accumulation (text content in content list)
          List<Map<String, Object>> currentContent = choice.getMessage().getContent();
          if (currentContent != null && !currentContent.isEmpty()) {
            mergeTextContent(currentContent, accumulated);
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

          // Handle tool_calls accumulation
          List<ToolCallBase> currentToolCalls = choice.getMessage().getToolCalls();
          if (currentToolCalls != null && !currentToolCalls.isEmpty()) {
            mergeToolCalls(currentToolCalls, accumulated.toolCalls);
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

  /**
   * Merges text content from current response with accumulated content.
   * For MultiModal, content is a List<Map<String, Object>> where text content is in maps with "text" key.
   */
  private void mergeTextContent(List<Map<String, Object>> currentContent, AccumulatedData accumulated) {
    for (Map<String, Object> contentItem : currentContent) {
      if (contentItem.containsKey("text")) {
        String textValue = (String) contentItem.get("text");
        if (textValue != null && !textValue.isEmpty()) {
          // Find or create text content item in accumulated content
          Map<String, Object> accumulatedTextItem = null;
          for (Map<String, Object> accItem : accumulated.content) {
            if (accItem.containsKey("text")) {
              accumulatedTextItem = accItem;
              break;
            }
          }

          if (accumulatedTextItem == null) {
            // Create new text content item
            accumulatedTextItem = new HashMap<>();
            accumulatedTextItem.put("text", textValue);
            accumulated.content.add(accumulatedTextItem);
          } else {
            // Append to existing text content
            String existingText = (String) accumulatedTextItem.get("text");
            if (existingText == null) {
              existingText = "";
            }
            accumulatedTextItem.put("text", existingText + textValue);
          }
        }
      }
    }
  }

  /**
   * Merges tool calls from current response with accumulated tool calls.
   */
  private void mergeToolCalls(List<ToolCallBase> currentToolCalls, List<ToolCallBase> accumulatedToolCalls) {
    for (ToolCallBase currentCall : currentToolCalls) {
      if (currentCall == null || currentCall.getIndex() == null) {
        continue;
      }

      int index = currentCall.getIndex();

      // Find existing accumulated call with same index
      ToolCallBase existingCall = null;
      for (ToolCallBase accCall : accumulatedToolCalls) {
        if (accCall != null && accCall.getIndex() != null && 
            accCall.getIndex().equals(index)) {
          existingCall = accCall;
          break;
        }
      }

      if (existingCall instanceof ToolCallFunction &&
              currentCall instanceof ToolCallFunction) {
        // Merge function calls
        ToolCallFunction existingFunctionCall = (ToolCallFunction) existingCall;
        ToolCallFunction currentFunctionCall = (ToolCallFunction) currentCall;

        if (currentFunctionCall.getFunction() != null) {
          // Ensure existing function call has a function object
          if (existingFunctionCall.getFunction() == null) {
            existingFunctionCall.setFunction(existingFunctionCall.new CallFunction());
          }

          // Accumulate arguments if present
          if (currentFunctionCall.getFunction().getArguments() != null) {
            String existingArguments = existingFunctionCall.getFunction().getArguments();
            if (existingArguments == null) {
              existingArguments = "";
            }
            String currentArguments = currentFunctionCall.getFunction().getArguments();
            existingFunctionCall.getFunction().setArguments(existingArguments + currentArguments);
          }

          // Accumulate function name if present
          if (currentFunctionCall.getFunction().getName() != null) {
            String existingName = existingFunctionCall.getFunction().getName();
            if (existingName == null) {
              existingName = "";
            }
            String currentName = currentFunctionCall.getFunction().getName();
            existingFunctionCall.getFunction().setName(existingName + currentName);
          }

          // Update function output if present
          if (currentFunctionCall.getFunction().getOutput() != null) {
            existingFunctionCall.getFunction().setOutput(currentFunctionCall.getFunction().getOutput());
          }
        }

        // Update other fields with latest non-empty values
        if (currentFunctionCall.getIndex() != null) {
          existingFunctionCall.setIndex(currentFunctionCall.getIndex());
        }
        if (currentFunctionCall.getId() != null && !currentFunctionCall.getId().isEmpty()) {
          existingFunctionCall.setId(currentFunctionCall.getId());
        }
        if (currentFunctionCall.getType() != null) {
          existingFunctionCall.setType(currentFunctionCall.getType());
        }
      } else {
        // Add new tool call (create a copy)
        if (currentCall instanceof ToolCallFunction) {
          ToolCallFunction currentFunctionCall = (ToolCallFunction) currentCall;
          ToolCallFunction newFunctionCall = new ToolCallFunction();
          newFunctionCall.setIndex(currentFunctionCall.getIndex());
          newFunctionCall.setId(currentFunctionCall.getId());
          newFunctionCall.setType(currentFunctionCall.getType());

          if (currentFunctionCall.getFunction() != null) {
            ToolCallFunction.CallFunction newCallFunction = newFunctionCall.new CallFunction();
            newCallFunction.setName(currentFunctionCall.getFunction().getName());
            newCallFunction.setArguments(currentFunctionCall.getFunction().getArguments());
            newCallFunction.setOutput(currentFunctionCall.getFunction().getOutput());
            newFunctionCall.setFunction(newCallFunction);
          }

          accumulatedToolCalls.add(newFunctionCall);
        } else {
          // For other types of tool calls, add directly (assuming they are immutable or don't need merging)
          accumulatedToolCalls.add(currentCall);
        }
      }
    }
  }

  /**
   * Clears accumulated data for the current thread.
   * Should be called when streaming is complete or encounters error.
   */
  private void clearAccumulatedData() {
    accumulatedDataMap.get().clear();
    accumulatedDataMap.remove();
  }

  /**
   * Inner class to store accumulated data for response merging.
   */
  private static class AccumulatedData {
    List<Map<String, Object>> content = new ArrayList<>();
    List<ToolCallBase> toolCalls = new ArrayList<>();
    StringBuilder reasoningContent = new StringBuilder();
  }
}
