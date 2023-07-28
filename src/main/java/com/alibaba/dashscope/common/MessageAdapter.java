// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class MessageAdapter extends TypeAdapter<Message> {
  @Override
  public void write(JsonWriter out, Message value) throws IOException {
    out.beginObject();
    out.name(ApiKeywords.ROLE);
    out.value(value.getRole());
    out.name(ApiKeywords.CONTENT);
    out.value(value.getContent());
    if (value.getExtras() != null && !value.getExtras().isEmpty()) {
      for (Entry<String, Object> entry : value.getExtras().entrySet()) {
        Object v = entry.getValue();
        String name = entry.getKey();
        out.name(name);
        if (v instanceof Integer) {
          out.value((Integer) v);
        } else if (v instanceof String) {
          out.value((String) v);
        } else if (v instanceof Double) {
          out.value((Double) v);
        } else if (v instanceof Boolean) {
          out.value((Boolean) v);
        } else {
          // Serialize object to string, when deserialize can't get object back.
          out.value(JsonUtils.toJson(v));
        }
      }
    }
    out.endObject();
  }

  @Override
  public Message read(JsonReader in) throws IOException {
    Map<String, Object> objectMap = JsonUtils.gson.fromJson(in, Map.class);
    Message msg = new Message();
    if (objectMap.containsKey(ApiKeywords.ROLE)) {
      msg.setRole((String) objectMap.get(ApiKeywords.ROLE));
      objectMap.remove(ApiKeywords.ROLE);
    }
    if (objectMap.containsKey(ApiKeywords.CONTENT)) {
      msg.setContent((String) objectMap.get(ApiKeywords.CONTENT));
      objectMap.remove(ApiKeywords.CONTENT);
    }
    msg.setExtras(objectMap);
    return msg;
  }
}
