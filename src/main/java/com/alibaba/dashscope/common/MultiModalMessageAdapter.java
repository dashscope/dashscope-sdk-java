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
        if (entry.getValue() instanceof String) {
          out.value((String) entry.getValue());
        } else if (entry.getValue() instanceof Integer) {
          out.value((Integer) (entry.getValue()));
        } else if (entry.getValue() instanceof Double) {
          out.value((Double) (entry.getValue()));
        } else if (entry.getValue() instanceof Boolean) {
          out.value((Boolean) (entry.getValue()));
        } else if (entry.getValue() instanceof Character) {
          out.value((Character) (entry.getValue()));
        } else if (entry.getValue() instanceof List) {
          out.beginArray();
          for (Object v : (List) entry.getValue()) {
            writePrimitiveType(out, v);
          }
          out.endArray();
        } else {
          writeMapObject(out, (Map<String, Object>) entry.getValue());
        }
      }
      out.endObject();
    }
  }

  private void writePrimitiveType(JsonWriter out, Object value) throws IOException {
    if (value instanceof String) {
      out.value((String) value);
    } else if (value instanceof Integer) {
      out.value((Integer) value);
    } else if (value instanceof Double) {
      out.value((Double) value);
    } else if (value instanceof Boolean) {
      out.value((Boolean) value);
    } else if (value instanceof Character) {
      out.value((Character) value);
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

    return msg;
  }
}
