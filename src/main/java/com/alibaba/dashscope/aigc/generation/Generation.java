// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.generation;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.ParamUtils;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;

@Slf4j
public final class Generation {
  private final SynchronizeHalfDuplexApi<HalfDuplexServiceParam> syncApi;
  private final ApiServiceOption serviceOption;

  private final ThreadLocal<Map<Integer, AccumulatedData>> accumulatedDataMap =
      ThreadLocal.withInitial(HashMap::new);

  public static class Models {
    /** @deprecated use QWEN_TURBO instead */
    @Deprecated public static final String QWEN_V1 = "qwen-v1";

    public static final String QWEN_TURBO = "qwen-turbo";

    public static final String BAILIAN_V1 = "bailian-v1";
    public static final String DOLLY_12B_V2 = "dolly-12b-v2";

    /** @deprecated use QWEN_PLUS instead */
    @Deprecated public static final String QWEN_PLUS_V1 = "qwen-plus-v1";

    public static final String QWEN_PLUS = "qwen-plus";
    public static final String QWEN_MAX = "qwen-max";
  }

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.AIGC.getValue())
        .task(Task.TEXT_GENERATION.getValue())
        .function(Function.GENERATION.getValue())
        .build();
  }

  public Generation() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Generation(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Generation(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (protocol.equals(Protocol.HTTP.getValue())) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Generation(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (protocol.equals(Protocol.HTTP.getValue())) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /**
   * Call the server to get the whole result, only http protocol
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws InputRequiredException Missing inputs.
   */
  public GenerationResult call(HalfDuplexServiceParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    return GenerationResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `GenerationParam`.
   * @param callback The callback to receive response, the template class is `GenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public void call(HalfDuplexServiceParam param, ResultCallback<GenerationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(GenerationResult.fromDashScopeResult(message));
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
   * Call the server to get the result by stream. http and websocket.
   *
   * @param param The input param of class `ConversationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public Flowable<GenerationResult> streamCall(HalfDuplexServiceParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();

    // Intercept and modify incrementalOutput parameter if needed
    boolean toMergeResponse = modifyIncrementalOutput(param);

    // Build custom user agent suffix with incremental_to_full flag
    int flagValue = toMergeResponse ? 1 : 0;
    String userAgentSuffix = String.format("incremental_to_full/%d", flagValue);
    param.putHeader("user-agent", userAgentSuffix);

    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    return syncApi.streamCall(param)
        .map(GenerationResult::fromDashScopeResult)
        .flatMap(result -> {
          GenerationResult merged =
              mergeSingleResponse(result, toMergeResponse, param);
          if (merged == null) {
            return Flowable.empty();
          }
          return Flowable.just(merged);
        })
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

  public void streamCall(HalfDuplexServiceParam param, ResultCallback<GenerationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();

    // Intercept and modify incrementalOutput parameter if needed
    boolean toMergeResponse = modifyIncrementalOutput(param);

    // Build custom user agent suffix with incremental_to_full flag
    int flagValue = toMergeResponse ? 1 : 0;
    String userAgentSuffix = String.format("incremental_to_full/%d", flagValue);
    param.putHeader("user-agent", userAgentSuffix);

    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            GenerationResult result = GenerationResult.fromDashScopeResult(msg);
            GenerationResult mergedResult = mergeSingleResponse(result, toMergeResponse, param);
            if (mergedResult != null) {
              callback.onEvent(mergedResult);
            }
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

  /**
   * Modifies the parameters for internal streaming optimization.
   * If incrementalOutput is false, modifies the GenerationParam object to set
   * incrementalOutput to true for internal streaming optimization.
   *
   * @param param The parameter object to modify
   * @return true if the parameter was modified, false otherwise
   */
  private boolean modifyIncrementalOutput(HalfDuplexServiceParam param) {
    // Check if the parameter is a GenerationParam and has incrementalOutput set to false
    if (param instanceof GenerationParam) {
      GenerationParam generationParam = (GenerationParam) param;
      Boolean incrementalOutput = generationParam.getIncrementalOutput();
      if (ParamUtils.shouldModifyIncrementalOutput(param.getModel()) &&
              Boolean.FALSE.equals(incrementalOutput)) {
        // Modify the GenerationParam object to enable incremental output
        generationParam.setIncrementalOutput(true);
        return true;
      }
    }
    return false;
  }

  /**
   * Merges a single GenerationResult with accumulated data for
   * non-incremental output simulation.
   * This method accumulates content and tool_calls from streaming responses.
   * Supports both legacy format (output.text) and new format
   * (output.choices[].message.content).
   *
   * @param result The GenerationResult to merge
   * @param toMergeResponse Whether to perform merging (based on original
   *                        incrementalOutput setting)
   * @param param The HalfDuplexServiceParam to get n parameter
   * @return The merged GenerationResult, or null if should be filtered out
   */
  private GenerationResult mergeSingleResponse(GenerationResult result,
      boolean toMergeResponse, HalfDuplexServiceParam param) {
    if (!toMergeResponse || result == null || result.getOutput() == null) {
      return result;
    }

    Map<Integer, AccumulatedData> accumulatedData = accumulatedDataMap.get();

    // Get n parameter
    Integer n = null;
    if (param instanceof GenerationParam) {
      n = ((GenerationParam) param).getN();
    }
    // Default n to 1 if not set
    if (n == null) {
      n = 1;
    }

    // Check if all choices have been sent (for n > 1 case)
    if (n > 1 && !accumulatedData.isEmpty()) {
      boolean allSent = accumulatedData.values().stream()
          .allMatch(data -> data.allChoicesSent);
      if (allSent) {
        return null;
      }
    }

    // Handle new format: output.choices[].message.content
    if (result.getOutput().getChoices() != null) {
      List<GenerationOutput.Choice> choices = result.getOutput().getChoices();

      // Filter out empty choices array
      if (choices.isEmpty()) {
        return null;
      }

      for (GenerationOutput.Choice choice : choices) {
        // Use the choice's index field for accumulation, fallback to 0
        Integer choiceIndex = choice.getIndex();
        if (choiceIndex == null) {
          choiceIndex = 0;
        }

        // Initialize accumulated data for this choice index if not exists
        AccumulatedData accumulated = accumulatedData.computeIfAbsent(
                choiceIndex, k -> new AccumulatedData());

        if (choice.getMessage() != null) {
          // Save role if present
          if (choice.getMessage().getRole() != null &&
              !choice.getMessage().getRole().isEmpty()) {
            accumulated.role = choice.getMessage().getRole();
          }

          // Handle content accumulation
          String currentContent = choice.getMessage().getContent();
          if (currentContent != null && !currentContent.isEmpty()) {
            accumulated.content.append(currentContent);
          }
          // Always set the accumulated content if we have any
          if (accumulated.content.length() > 0) {
            choice.getMessage().setContent(accumulated.content.toString());
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

          // Restore role if we have it
          if (accumulated.role != null &&
              (choice.getMessage().getRole() == null ||
               choice.getMessage().getRole().isEmpty())) {
            choice.getMessage().setRole(accumulated.role);
          }
        }

        // Handle logprobs accumulation
        if (choice.getLogprobs() != null && choice.getLogprobs().getContent() != null) {
          List<GenerationLogprobs.Content> currentLogprobsContent = choice.getLogprobs().getContent();
          if (!currentLogprobsContent.isEmpty()) {
            accumulated.logprobsContent.addAll(currentLogprobsContent);
          }
        }
        // Always set accumulated logprobs if we have any
        if (!accumulated.logprobsContent.isEmpty() && choice.getLogprobs() != null) {
          choice.getLogprobs().setContent(accumulated.logprobsContent);
        }

        // Handle finish_reason for n > 1 case
        if (n > 1 && choice.getFinishReason() != null &&
            !choice.getFinishReason().equals("null")) {
          accumulated.finishReason = choice.getFinishReason();
          accumulated.finished = true;
        }
      }

      // Store output_tokens for each choice when n > 1
      // Each streaming packet contains usage info for one specific choice
      if (n > 1 && result.getUsage() != null &&
          result.getUsage().getOutputTokens() != null &&
          !choices.isEmpty()) {
        // Get the choice index from the first choice in this packet
        Integer choiceIndex = choices.get(0).getIndex();
        if (choiceIndex == null) {
          choiceIndex = 0;
        }
        AccumulatedData accumulated = accumulatedData.get(choiceIndex);
        if (accumulated != null) {
          accumulated.outputTokens = result.getUsage().getOutputTokens();
        }
      }

      // Handle n > 1 case: different strategies for different
      // finish_reason
      if (n > 1) {
        // Count finished choices
        int finishedCount = 0;
        for (AccumulatedData data : accumulatedData.values()) {
          if (data.finished) {
            finishedCount++;
          }
        }

        // Find current packet's finished choice (if any)
        String currentFinishReason = null;
        Integer currentChoiceIndex = null;
        for (GenerationOutput.Choice choice : choices) {
          if (choice.getFinishReason() != null &&
              !choice.getFinishReason().equals("null")) {
            currentFinishReason = choice.getFinishReason();
            currentChoiceIndex =
                choice.getIndex() != null ? choice.getIndex() : 0;
            break;
          }
        }

        // No finish_reason in current packet: return as is
        if (currentFinishReason == null) {
          return result;
        }

        // For stop: wait all choices, then merge into one result
        if ("stop".equals(currentFinishReason)) {
          if (finishedCount < n) {
            // Hide finish_reason until all finished
            for (GenerationOutput.Choice choice : choices) {
              if (choice.getFinishReason() != null &&
                  !choice.getFinishReason().equals("null")) {
                choice.setFinishReason("null");
              }
            }
          } else {
            // All finished: merge all choices into one result
            for (AccumulatedData data : accumulatedData.values()) {
              data.allChoicesSent = true;
            }
            GenerationOutput output = result.getOutput();
            List<GenerationOutput.Choice> allChoices = new ArrayList<>();
            int totalOutputTokens = 0;
            for (Map.Entry<Integer, AccumulatedData> entry :
                accumulatedData.entrySet()) {
              Integer index = entry.getKey();
              AccumulatedData data = entry.getValue();
              GenerationOutput.Choice finalChoice = output.new Choice();
              finalChoice.setIndex(index);
              finalChoice.setFinishReason(data.finishReason);
              com.alibaba.dashscope.common.Message message =
                  new com.alibaba.dashscope.common.Message();
              message.setRole("assistant");
              if (data.content.length() > 0) {
                message.setContent(data.content.toString());
              }
              if (data.reasoningContent.length() > 0) {
                message.setReasoningContent(data.reasoningContent.toString());
              }
              if (!data.toolCalls.isEmpty()) {
                message.setToolCalls(data.toolCalls);
              }
              finalChoice.setMessage(message);
              if (!data.logprobsContent.isEmpty()) {
                GenerationLogprobs logprobs = new GenerationLogprobs();
                logprobs.setContent(new ArrayList<>(data.logprobsContent));
                finalChoice.setLogprobs(logprobs);
              }
              allChoices.add(finalChoice);
              if (data.outputTokens != null) {
                totalOutputTokens += data.outputTokens;
              }
            }
            output.setChoices(allChoices);
            if (result.getUsage() != null && totalOutputTokens > 0) {
              result.getUsage().setOutputTokens(totalOutputTokens);
              if (result.getUsage().getInputTokens() != null) {
                result.getUsage().setTotalTokens(
                    result.getUsage().getInputTokens() + totalOutputTokens);
              }
            }
          }
        } else {
          // For non-stop (e.g., tool_calls): output each choice separately
          AccumulatedData currentData = accumulatedData.get(currentChoiceIndex);
          if (currentData == null || currentData.allChoicesSent) {
            return null;
          }
          currentData.allChoicesSent = true;
          // Reuse current choice in result, just update it
          for (GenerationOutput.Choice choice : choices) {
            if (choice.getIndex() != null &&
                choice.getIndex().equals(currentChoiceIndex)) {
              // Update usage with this choice's output tokens
              if (result.getUsage() != null && currentData.outputTokens != null) {
                result.getUsage().setOutputTokens(currentData.outputTokens);
                if (result.getUsage().getInputTokens() != null) {
                  result.getUsage().setTotalTokens(
                      result.getUsage().getInputTokens() +
                      currentData.outputTokens);
                }
              }
              return result;
            }
          }
        }
      }
    }
    // Handle legacy format: output.text
    else {
      // Use choice index 0 for legacy format
      AccumulatedData accumulated = accumulatedData.computeIfAbsent(0, k -> new AccumulatedData());

      String currentText = result.getOutput().getText();
      if (currentText != null && !currentText.isEmpty()) {
        accumulated.content.append(currentText);
      }
      // Always set the accumulated content if we have any
      if (accumulated.content.length() > 0) {
        result.getOutput().setText(accumulated.content.toString());
      }
    }

    return result;
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
    StringBuilder content = new StringBuilder();
    StringBuilder reasoningContent = new StringBuilder();
    List<ToolCallBase> toolCalls = new ArrayList<>();
    List<GenerationLogprobs.Content> logprobsContent = new ArrayList<>();
    boolean finished = false;
    String finishReason = null;
    boolean allChoicesSent = false;
    String role = null;
    Integer outputTokens = null;
  }
}
