package com.alibaba.dashscope.tools;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;

public class ToolCallGsonDeserializer implements JsonDeserializer<Object> {
  @Override
  public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (json.getAsJsonObject().get("type") == null) {
      return json.toString();
    }
    String toolTypeName = json.getAsJsonObject().get("type").getAsString();
    Type toolType = ToolCallBase.getToolCallClass(toolTypeName);
    if (toolType == null) {
      return json.toString();
    }
    return context.deserialize(json, toolType);
  }
}
