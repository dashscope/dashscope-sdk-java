// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

/**
 * AgentStudio-owned Gson helpers for the HTTP transport stack.
 *
 * <p>Number and escaping policies match the former shared utility: {@code LONG_OR_DOUBLE} number
 * parsing, disabled HTML escaping and {@link GsonExclude} field exclusion. Model responses with
 * polymorphic content blocks are parsed through {@code AgentStudioJson.GSON} instead.
 */
public class JsonUtils {
  public static final Gson gson =
      new GsonBuilder()
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
    return parseString(jsonString).getAsJsonObject();
  }

  // Copy from Gson JsonParser. to relax gson version requirement
  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param json JSON text
   * @return a parse tree of {@link JsonElement}s corresponding to the specified JSON
   * @throws JsonParseException if the specified text is not valid JSON
   */
  public static JsonElement parseString(String json) throws JsonSyntaxException {
    return parseReader(new StringReader(json));
  }

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param reader JSON text
   * @return a parse tree of {@link JsonElement}s corresponding to the specified JSON
   * @throws JsonParseException if the specified text is not valid JSON
   */
  public static JsonElement parseReader(Reader reader) throws JsonIOException, JsonSyntaxException {
    try {
      JsonReader jsonReader = new JsonReader(reader);
      JsonElement element = parseReader(jsonReader);
      if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
        throw new JsonSyntaxException("Did not consume the entire document.");
      }
      return element;
    } catch (MalformedJsonException e) {
      throw new JsonSyntaxException(e);
    } catch (IOException e) {
      throw new JsonIOException(e);
    } catch (NumberFormatException e) {
      throw new JsonSyntaxException(e);
    }
  }

  /**
   * Returns the next value from the JSON stream as a parse tree.
   *
   * @param reader The json reader.
   * @throws JsonParseException if there is an IOException or if the specified text is not valid
   *     JSON
   * @return The `JsonElement` of the reader.
   */
  public static JsonElement parseReader(JsonReader reader)
      throws JsonIOException, JsonSyntaxException {
    boolean lenient = reader.isLenient();
    reader.setLenient(true);
    try {
      return Streams.parse(reader);
    } catch (StackOverflowError e) {
      throw new JsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
    } catch (OutOfMemoryError e) {
      throw new JsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
    } finally {
      reader.setLenient(lenient);
    }
  }
}
