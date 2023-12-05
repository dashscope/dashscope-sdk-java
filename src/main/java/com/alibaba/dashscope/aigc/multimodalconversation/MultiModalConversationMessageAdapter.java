package com.alibaba.dashscope.aigc.multimodalconversation;

import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MultiModalConversationMessageAdapter
    extends TypeAdapter<MultiModalConversationMessage> {
  @Override
  public void write(JsonWriter out, MultiModalConversationMessage value) throws IOException {
    out.beginObject();
    out.name(ApiKeywords.ROLE);
    out.value(value.getRole());
    out.name(ApiKeywords.CONTENT);
    out.beginArray();
    for (MultiModalMessageItemBase item : value.content) {
      out.beginObject();
      out.name(item.getModal());
      out.value(item.getContent());
      out.endObject();
    }
    out.endArray();
    out.endObject();
  }

  @Override
  public MultiModalConversationMessage read(JsonReader in) throws IOException {
    Map<String, Object> objectMap = JsonUtils.gson.fromJson(in, Map.class);
    MultiModalConversationMessage msg = new MultiModalConversationMessage();
    if (objectMap.containsKey(ApiKeywords.ROLE)) {
      msg.setRole((String) objectMap.get(ApiKeywords.ROLE));
      objectMap.remove(ApiKeywords.ROLE);
    }
    if (objectMap.containsKey(ApiKeywords.CONTENT)) {
      msg.setContent((List<MultiModalMessageItemBase>) objectMap.get(ApiKeywords.CONTENT));
      objectMap.remove(ApiKeywords.CONTENT);
    }
    return msg;
  }
}
