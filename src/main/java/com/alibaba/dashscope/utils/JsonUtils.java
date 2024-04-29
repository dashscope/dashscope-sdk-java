// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationMessage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationMessageAdapter;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageAdapter;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.MultiModalMessageAdapter;
import com.alibaba.dashscope.threads.AnnotationBase;
import com.alibaba.dashscope.threads.AnnotationDeserializer;
import com.alibaba.dashscope.threads.ContentBase;
import com.alibaba.dashscope.threads.MessageContentDeserializer;
import com.alibaba.dashscope.threads.runs.StepDetailBase;
import com.alibaba.dashscope.threads.runs.StepDetailDeserializer;
import com.alibaba.dashscope.tools.ToolBase;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallGsonDeserializer;
import com.alibaba.dashscope.tools.ToolGsonDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class JsonUtils {
  public static final Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(Message.class, new MessageAdapter())
          .registerTypeAdapter(
              MultiModalConversationMessage.class, new MultiModalConversationMessageAdapter())
          .registerTypeAdapter(MultiModalMessage.class, new MultiModalMessageAdapter())
          .registerTypeAdapter(ToolBase.class, new ToolGsonDeserializer())
          .registerTypeAdapter(ContentBase.class, new MessageContentDeserializer())
          .registerTypeAdapter(AnnotationBase.class, new AnnotationDeserializer())
          .registerTypeAdapter(StepDetailBase.class, new StepDetailDeserializer())
          .registerTypeAdapter(ToolCallBase.class, new ToolCallGsonDeserializer())
          .addSerializationExclusionStrategy(new AnnotationExclusionStrategy())
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
