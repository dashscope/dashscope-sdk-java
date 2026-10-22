// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.utils;

import com.alibaba.dashscope.resources.agentstudio.message.ContentBlock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.ToNumberPolicy;
import java.lang.reflect.Type;

/**
 * AgentStudio-scoped 3.0 copy of the 2.x JsonUtils: same Gson configuration (LONG_OR_DOUBLE number
 * strategy, HTML escaping off, {@link GsonExclude} fields skipped) but registers only the
 * agentstudio {@link ContentBlock} polymorphic adapter — the 2.x instance also wires threads/tools
 * /aigc adapters that this subtree never serializes.
 */
public class JsonUtils {
  public static final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(ContentBlock.class, new ContentBlock.Deserializer())
          .addSerializationExclusionStrategy(new AnnotationExclusionStrategy())
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

  public static JsonArray toJsonArray(Object obj) {
    return gson.toJsonTree(obj).getAsJsonArray();
  }

  public static JsonObject toJsonObject(Object obj) {
    return gson.toJsonTree(obj).getAsJsonObject();
  }

  public static JsonElement toJsonElement(Object obj) {
    return gson.toJsonTree(obj);
  }

  public static JsonObject parse(String jsonString) throws JsonParseException {
    return gson.fromJson(jsonString, JsonObject.class);
  }
}
