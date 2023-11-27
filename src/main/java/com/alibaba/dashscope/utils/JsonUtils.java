// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationMessage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageAdapter;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.ToNumberPolicy;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonUtils {
  public static final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(Message.class, new MessageAdapter())
          .registerTypeAdapter(MultiModalConversationMessage.class, new MultiModalMessageAdapter())
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .disableHtmlEscaping()
          .create();

  public static String toJson(Object obj) {
    return gson.toJson(obj);
  }

  public static <T> T fromJson(String obj, Class<T> clazz) {
    return gson.fromJson(obj, clazz);
  }

  public static <T> T fromJson(String str, Type typeOfT) {
    return gson.fromJson(str, typeOfT);
  }

  public static <T> T fromJson(JsonElement json, Type typeOfT) {
    return gson.fromJson(json, typeOfT);
  }

  public static <T> T fromJsonObject(JsonElement jsonElement, Class<T> clazz) {
    return gson.fromJson(jsonElement, clazz);
  }

  public static JsonArray toJsonArray(Object obj) {
    return gson.toJsonTree(obj).getAsJsonArray();
  }

  public static JsonObject toJsonObject(Object obj) {
    return gson.toJsonTree(obj).getAsJsonObject();
  }

  public static JsonElement toJsonElement(Object obj) {
    return gson.toJsonTree(obj);
  }

  public static JsonObject parse(String jsonString) {
    return JsonParser.parseString(jsonString).getAsJsonObject();
  }

  public static JsonObject merge(JsonObject dest, JsonObject src) {
    for (Map.Entry<String, JsonElement> srcEntry : src.entrySet()) {
      String key = srcEntry.getKey();
      JsonElement value = srcEntry.getValue();
      if (!dest.has(key)) {
        dest.add(key, value);
      }
    }
    return dest;
  }

  public static JsonObject parametersToJsonObject(Map<String, Object> parameters) {
    if (parameters != null) {
      JsonObject jsonObject = new JsonObject();
      for (Map.Entry<String, Object> entry : parameters.entrySet()) {
        if (entry.getValue() instanceof String) {
          jsonObject.addProperty(entry.getKey(), (String) (entry.getValue()));
        } else if (entry.getValue() instanceof Integer) {
          jsonObject.addProperty(entry.getKey(), (Integer) (entry.getValue()));
        } else if (entry.getValue() instanceof Double) {
          jsonObject.addProperty(entry.getKey(), (Double) (entry.getValue()));
        } else if (entry.getValue() instanceof Boolean) {
          jsonObject.addProperty(entry.getKey(), (Boolean) (entry.getValue()));
        } else if (entry.getValue() instanceof Character) {
          jsonObject.addProperty(entry.getKey(), (Character) (entry.getValue()));
        } else if (entry.getValue() instanceof List<?>) {
          jsonObject.add(entry.getKey(), JsonUtils.toJsonArray(entry.getValue()));
        } else if (entry.getValue() instanceof Map<?, ?>) {
          jsonObject.add(entry.getKey(), JsonUtils.toJsonObject(entry.getValue()));
        } else {
          jsonObject.add(entry.getKey(), JsonUtils.toJsonElement(entry.getValue()));
        }
      }
      return jsonObject;
    } else {
      return null;
    }
  }
}
