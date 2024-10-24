// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

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
    return msg;
  }
}
