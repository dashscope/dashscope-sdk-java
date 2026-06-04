// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StreamingMerger {

  private StreamingMerger() {}

  public static <T> void clearAccumulatedData(ThreadLocal<Map<Integer, T>> accumulatedDataMap) {
    accumulatedDataMap.get().clear();
    accumulatedDataMap.remove();
  }

  public static void mergeTextContent(
      List<Map<String, Object>> currentContent, List<Map<String, Object>> accumulatedContent) {
    for (Map<String, Object> contentItem : currentContent) {
      if (contentItem.containsKey("text")) {
        String textValue = (String) contentItem.get("text");
        if (textValue != null && !textValue.isEmpty()) {
          Map<String, Object> accumulatedTextItem = null;
          for (Map<String, Object> accItem : accumulatedContent) {
            if (accItem.containsKey("text")) {
              accumulatedTextItem = accItem;
              break;
            }
          }

          if (accumulatedTextItem == null) {
            accumulatedTextItem = new HashMap<>();
            accumulatedTextItem.put("text", textValue);
            accumulatedContent.add(accumulatedTextItem);
          } else {
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

  public static void mergeToolCalls(
      List<ToolCallBase> currentToolCalls, List<ToolCallBase> accumulatedToolCalls) {
    for (ToolCallBase currentCall : currentToolCalls) {
      if (currentCall == null || currentCall.getIndex() == null) {
        continue;
      }

      int index = currentCall.getIndex();

      ToolCallBase existingCall = null;
      for (ToolCallBase accCall : accumulatedToolCalls) {
        if (accCall != null && accCall.getIndex() != null && accCall.getIndex().equals(index)) {
          existingCall = accCall;
          break;
        }
      }

      if (existingCall instanceof ToolCallFunction && currentCall instanceof ToolCallFunction) {
        ToolCallFunction existingFunctionCall = (ToolCallFunction) existingCall;
        ToolCallFunction currentFunctionCall = (ToolCallFunction) currentCall;

        if (currentFunctionCall.getFunction() != null) {
          if (existingFunctionCall.getFunction() == null) {
            existingFunctionCall.setFunction(existingFunctionCall.new CallFunction());
          }

          if (currentFunctionCall.getFunction().getArguments() != null) {
            String existingArguments = existingFunctionCall.getFunction().getArguments();
            if (existingArguments == null) {
              existingArguments = "";
            }
            String currentArguments = currentFunctionCall.getFunction().getArguments();
            existingFunctionCall.getFunction().setArguments(existingArguments + currentArguments);
          }

          if (currentFunctionCall.getFunction().getName() != null) {
            String existingName = existingFunctionCall.getFunction().getName();
            if (existingName == null) {
              existingName = "";
            }
            String currentName = currentFunctionCall.getFunction().getName();
            existingFunctionCall.getFunction().setName(existingName + currentName);
          }

          if (currentFunctionCall.getFunction().getOutput() != null) {
            existingFunctionCall
                .getFunction()
                .setOutput(currentFunctionCall.getFunction().getOutput());
          }
        }

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
          accumulatedToolCalls.add(currentCall);
        }
      }
    }
  }
}
