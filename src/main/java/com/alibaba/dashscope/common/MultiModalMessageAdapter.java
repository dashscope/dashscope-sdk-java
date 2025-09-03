// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.codeinterpretertool.ToolCallCodeInterpreter;
import com.alibaba.dashscope.tools.search.ToolCallQuarkSearch;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MultiModalMessageAdapter extends TypeAdapter<MultiModalMessage> {
  @SuppressWarnings({"rawtypes", "unchecked"})
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

  private void writeValue(JsonWriter out, Object value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else if (value instanceof String) {
      out.value((String) value);
    } else if (value instanceof Integer) {
      out.value((Integer) value);
    } else if (value instanceof Double) {
      out.value((Double) value);
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
      // Fallback for other types
      out.value(value.toString());
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
      // For ToolCallCodeInterpreter no extra fields besides id and type
      // Any additional fields specific to this should be written here.
    }

    writer.endObject();
  }


  @Override
  public void write(JsonWriter out, MultiModalMessage value) throws IOException {
    out.beginObject();
    out.name(ApiKeywords.ROLE);
    out.value(value.getRole());

    out.name(ApiKeywords.CONTENT);
    out.beginArray();
    for (Map<String, Object> item : value.getContent()) {
      writeMapObject(out, item);
    }
    out.endArray();

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
      for (ToolCallBase tc : JsonUtils.fromJson(JsonUtils.toJson(toolCalls), ToolCallBase[].class)) {
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
      } else {
        msg.setContent((List<Map<String, Object>>) content);
      }
      objectMap.remove(ApiKeywords.CONTENT);
    }

    if (objectMap.containsKey(ApiKeywords.ANNOTATIONS)) {
      msg.setAnnotations((List<Map<String, Object>>) objectMap.get(ApiKeywords.ANNOTATIONS));
      objectMap.remove(ApiKeywords.ANNOTATIONS);
    }

    if (objectMap.containsKey(ApiKeywords.REASONING_CONTENT)) {
      String reasoningContent = (String) objectMap.get(ApiKeywords.REASONING_CONTENT);
      msg.setReasoningContent(reasoningContent);
      objectMap.remove(ApiKeywords.REASONING_CONTENT);
    }

    if (objectMap.containsKey(ApiKeywords.TOOL_CALLS)) {
      Object toolCalls = objectMap.get(ApiKeywords.TOOL_CALLS);
      msg.setToolCalls((List<ToolCallBase>) toolCalls);
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
