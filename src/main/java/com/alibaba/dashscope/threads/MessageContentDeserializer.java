package com.alibaba.dashscope.threads;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

public class MessageContentDeserializer implements JsonDeserializer<Object> {

  @Override
  public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    String typeName = json.getAsJsonObject().get("type").getAsString();
    Type toolType = ContentBase.getContentClass(typeName);
    if (toolType == null) {
      return json.toString();
    }
    return context.deserialize(json, toolType);
  }
}
