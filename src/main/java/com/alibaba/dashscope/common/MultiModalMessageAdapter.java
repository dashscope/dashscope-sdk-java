// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.codeinterpretertool.ToolCallCodeInterpreter;
import com.alibaba.dashscope.tools.search.ToolCallQuarkSearch;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiModalMessageAdapter extends TypeAdapter<MultiModalMessage> {
  private void writeMapObject(JsonWriter out, Map<String, Object> mapObject) throws IOException {
    if (mapObject != null) {
      out.beginObject();
      for (Map.Entry<String, Object> entry : mapObject.entrySet()) {
        out.name(entry.getKey());
        writeValue(out, entry.getValue());
      }
      out.endObject();
    }
  }

  @SuppressWarnings("unchecked")
  private void writeValue(JsonWriter out, Object value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else if (value instanceof String) {
      out.value((String) value);
    } else if (value instanceof Integer || value instanceof Long) {
      out.value(((Number) value).longValue());
    } else if (value instanceof Float || value instanceof Double) {
      out.value(((Number) value).doubleValue());
    } else if (value instanceof Short || value instanceof Byte) {
      out.value(((Number) value).intValue());
    } else if (value instanceof BigDecimal || value instanceof BigInteger) {
      // value(Number) keeps the JSON number unquoted and is JsonTreeWriter-safe,
      // unlike jsonValue which throws UnsupportedOperationException under toJsonTree
      out.value((Number) value);
    } else if (value instanceof Boolean) {
      out.value((Boolean) value);
    } else if (value instanceof Character) {
      out.value((Character) value);
    } else if (value instanceof List) {
      out.beginArray();
      List<?> list = (List<?>) value;
      for (Object item : list) {
        writeValue(out, item);
      }
      out.endArray();
    } else if (value instanceof Map) {
      writeMapObject(out, (Map<String, Object>) value);
    } else {
      // Serialize arbitrary objects through Gson directly into the writer;
      // works for both streaming and JsonTreeWriter (toJsonTree) paths
      JsonUtils.gson.toJson(value, value.getClass(), out);
    }
  }

  private void writeToolCallBase(JsonWriter writer, ToolCallBase toolCallBase) throws IOException {
    writer.beginObject();

    // Write common fields
    writer.name("id").value(toolCallBase.getId());
    writer.name("type").value(toolCallBase.getType());
    if (toolCallBase.getIndex() != null) {
      writer.name("index").value(toolCallBase.getIndex());
    }

    // Handle specific subclass serialization
    if (toolCallBase instanceof ToolCallFunction) {
      ToolCallFunction functionCall = (ToolCallFunction) toolCallBase;
      ToolCallFunction.CallFunction callFunction = functionCall.getFunction();
      writer.name("function").beginObject();
      if (callFunction != null) {
        writer.name("name").value(callFunction.getName());
        writer.name("arguments").value(callFunction.getArguments());
        writer.name("output").value(callFunction.getOutput());
      }
      writer.endObject();
    } else if (toolCallBase instanceof ToolCallQuarkSearch) {
      ToolCallQuarkSearch quarkSearchCall = (ToolCallQuarkSearch) toolCallBase;
      writer.name("quark_search").beginObject();
      if (quarkSearchCall.getQuarkSearch() != null) {
        for (Map.Entry<String, String> entry : quarkSearchCall.getQuarkSearch().entrySet()) {
          writer.name(entry.getKey()).value(entry.getValue());
        }
      }
      writer.endObject();
    } else if (toolCallBase instanceof ToolCallCodeInterpreter) {
      // ToolCallCodeInterpreter only has id, type, and index fields
      // id and type are already written above, index is handled in common fields
    }

    writer.endObject();
  }

  // Convert LinkedTreeMap to ToolCallFunction
  @SuppressWarnings("unchecked")
  private ToolCallFunction convertToCallFunction(LinkedTreeMap<String, Object> toolCall) {
    ToolCallFunction functionCall = new ToolCallFunction();
    if (toolCall.containsKey("function")) {
      ToolCallFunction.CallFunction callFunction = functionCall.new CallFunction();
      LinkedTreeMap<String, Object> fc = (LinkedTreeMap<String, Object>) toolCall.get("function");
      if (fc.containsKey("name")) {
        callFunction.setName(fc.get("name").toString());
      }
      if (fc.containsKey("arguments")) {
        Object args = fc.get("arguments");
        callFunction.setArguments(args instanceof String ? (String) args : JsonUtils.toJson(args));
      }
      if (fc.containsKey("output")) {
        Object output = fc.get("output");
        callFunction.setOutput(
            output instanceof String ? (String) output : JsonUtils.toJson(output));
      }
      functionCall.setFunction(callFunction);
    }
    functionCall.setType(toolCall.get("type").toString());
    if (toolCall.containsKey("id")) {
      functionCall.setId(toolCall.get("id").toString());
    }
    if (toolCall.containsKey("index")) {
      Object indexObj = toolCall.get("index");
      if (indexObj instanceof Number) {
        functionCall.setIndex(((Number) indexObj).intValue());
      }
    }
    return functionCall;
  }

  // Convert LinkedTreeMap to ToolCallQuarkSearch
  @SuppressWarnings("unchecked")
  private ToolCallQuarkSearch convertToQuarkSearch(LinkedTreeMap<String, Object> toolCall) {
    ToolCallQuarkSearch quarkSearch = new ToolCallQuarkSearch();
    if (toolCall.containsKey("quark_search")) {
      LinkedTreeMap<String, Object> qs =
          (LinkedTreeMap<String, Object>) toolCall.get("quark_search");
      Map<String, String> searchParams = new HashMap<>();
      for (Map.Entry<String, Object> entry : qs.entrySet()) {
        Object val = entry.getValue();
        if (val instanceof String) {
          searchParams.put(entry.getKey(), (String) val);
        } else {
          // For non-string types, serialize to JSON string
          searchParams.put(entry.getKey(), JsonUtils.toJson(val));
        }
      }
      quarkSearch.setQuarkSearch(searchParams);
    }
    quarkSearch.setType(toolCall.get("type").toString());
    if (toolCall.containsKey("id")) {
      quarkSearch.setId(toolCall.get("id").toString());
    }
    if (toolCall.containsKey("index")) {
      Object indexObj = toolCall.get("index");
      if (indexObj instanceof Number) {
        quarkSearch.setIndex(((Number) indexObj).intValue());
      }
    }
    return quarkSearch;
  }

  // Convert LinkedTreeMap to ToolCallCodeInterpreter
  @SuppressWarnings("unchecked")
  private ToolCallCodeInterpreter convertToCodeInterpreter(LinkedTreeMap<String, Object> toolCall) {
    ToolCallCodeInterpreter codeInterpreter = new ToolCallCodeInterpreter();
    codeInterpreter.setType(toolCall.get("type").toString());
    if (toolCall.containsKey("id")) {
      codeInterpreter.setId(toolCall.get("id").toString());
    }
    if (toolCall.containsKey("index")) {
      Object indexObj = toolCall.get("index");
      if (indexObj instanceof Number) {
        codeInterpreter.setIndex(((Number) indexObj).intValue());
      }
    }
    return codeInterpreter;
  }

  // Generic method to convert LinkedTreeMap to appropriate ToolCallBase subclass
  @SuppressWarnings("unchecked")
  private ToolCallBase convertToToolCall(LinkedTreeMap<String, Object> toolCall) {
    if (!toolCall.containsKey("type")) {
      throw new IllegalArgumentException("Tool call must contain 'type' field");
    }

    String type = toolCall.get("type").toString();
    switch (type) {
      case "function":
        return convertToCallFunction(toolCall);
      case "quark_search":
        return convertToQuarkSearch(toolCall);
      case "code_interpreter":
        return convertToCodeInterpreter(toolCall);
      default:
        throw new IllegalArgumentException("Unknown tool call type: " + type);
    }
  }

  @Override
  public void write(JsonWriter out, MultiModalMessage value) throws IOException {
    out.beginObject();
    out.name(ApiKeywords.ROLE);
    out.value(value.getRole());

    if (value.getContent() != null) {
      out.name(ApiKeywords.CONTENT);
      out.beginArray();
      for (Map<String, Object> item : value.getContent()) {
        writeMapObject(out, item);
      }
      out.endArray();
    }

    if (value.getAnnotations() != null) {
      out.name(ApiKeywords.ANNOTATIONS);
      out.beginArray();
      for (Map<String, Object> item : value.getAnnotations()) {
        writeMapObject(out, item);
      }
      out.endArray();
    }

    if (value.getReasoningContent() != null) {
      out.name(ApiKeywords.REASONING_CONTENT);
      out.value(value.getReasoningContent());
    }

    if (value.getToolCalls() != null) {
      out.name(ApiKeywords.TOOL_CALLS);
      out.beginArray();
      List<ToolCallBase> toolCalls = value.getToolCalls();
      for (ToolCallBase tc : toolCalls) {
        writeToolCallBase(out, tc);
      }
      out.endArray();
    }

    if (value.getToolCallId() != null) {
      out.name(ApiKeywords.TOOL_CALL_ID);
      out.value(value.getToolCallId());
    }

    if (value.getName() != null) {
      out.name(ApiKeywords.NAME);
      out.value(value.getName());
    }

    out.endObject();
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public MultiModalMessage read(JsonReader in) throws IOException {
    Map<String, Object> objectMap = JsonUtils.gson.fromJson(in, Map.class);
    MultiModalMessage msg = new MultiModalMessage();

    if (objectMap.containsKey(ApiKeywords.ROLE)) {
      msg.setRole((String) objectMap.get(ApiKeywords.ROLE));
      objectMap.remove(ApiKeywords.ROLE);
    }

    if (objectMap.containsKey(ApiKeywords.CONTENT)) {
      Object content = objectMap.get(ApiKeywords.CONTENT);
      if (content instanceof String) {
        msg.setContent(Arrays.asList(Collections.singletonMap("text", (String) content)));
      } else if (content instanceof List) {
        msg.setContent((List<Map<String, Object>>) content);
      } else {
        throw new IllegalArgumentException(
            "Content must be String or List, got: "
                + (content != null ? content.getClass().getName() : "null"));
      }
      objectMap.remove(ApiKeywords.CONTENT);
    }

    if (objectMap.containsKey(ApiKeywords.ANNOTATIONS)) {
      Object annotations = objectMap.get(ApiKeywords.ANNOTATIONS);
      if (annotations instanceof List) {
        msg.setAnnotations((List<Map<String, Object>>) annotations);
      } else {
        throw new IllegalArgumentException(
            "Annotations must be List, got: "
                + (annotations != null ? annotations.getClass().getName() : "null"));
      }
      objectMap.remove(ApiKeywords.ANNOTATIONS);
    }

    if (objectMap.containsKey(ApiKeywords.REASONING_CONTENT)) {
      Object reasoningContent = objectMap.get(ApiKeywords.REASONING_CONTENT);
      if (reasoningContent instanceof String) {
        msg.setReasoningContent((String) reasoningContent);
      } else {
        throw new IllegalArgumentException(
            "Reasoning content must be String, got: "
                + (reasoningContent != null ? reasoningContent.getClass().getName() : "null"));
      }
      objectMap.remove(ApiKeywords.REASONING_CONTENT);
    }

    if (objectMap.containsKey(ApiKeywords.TOOL_CALLS)) {
      Object toolCallsObj = objectMap.get(ApiKeywords.TOOL_CALLS);
      if (toolCallsObj instanceof List) {
        List<?> toolCallsList = (List<?>) toolCallsObj;
        // Check if need conversion for function type
        boolean needConversion = false;
        if (!toolCallsList.isEmpty() && toolCallsList.get(0) instanceof LinkedTreeMap) {
          needConversion = true;
        }

        if (needConversion) {
          // Convert LinkedTreeMap to appropriate ToolCallBase subclass
          msg.toolCalls = new ArrayList<ToolCallBase>();
          List<LinkedTreeMap> toolCalls = (List<LinkedTreeMap>) toolCallsObj;
          for (LinkedTreeMap<String, Object> toolCall : toolCalls) {
            msg.toolCalls.add(convertToToolCall(toolCall));
          }
        } else {
          // Use original method for non-function types
          msg.setToolCalls((List<ToolCallBase>) toolCallsObj);
        }
      }
      objectMap.remove(ApiKeywords.TOOL_CALLS);
    }

    if (objectMap.containsKey(ApiKeywords.TOOL_CALL_ID)) {
      String toolCallId = (String) objectMap.get(ApiKeywords.TOOL_CALL_ID);
      msg.setToolCallId(toolCallId);
      objectMap.remove(ApiKeywords.TOOL_CALL_ID);
    }

    if (objectMap.containsKey(ApiKeywords.NAME)) {
      String name = (String) objectMap.get(ApiKeywords.NAME);
      msg.setName(name);
      objectMap.remove(ApiKeywords.NAME);
    }

    return msg;
  }
}
