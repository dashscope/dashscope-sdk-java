// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils {
  private static final Gson gson = new Gson();

  public static String toJson(Object obj) {
    return gson.toJson(obj);
  }

  public static <T> T fromJson(String obj, Class<T> clazz) {
    return gson.fromJson(obj, clazz);
  }

  public static JsonObject toJsonObject(Object obj) {
    return gson.toJsonTree(obj).getAsJsonObject();
  }

  public static JsonObject parse(String jsonString) {
    return JsonParser.parseString(jsonString).getAsJsonObject();
  }
}
