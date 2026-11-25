package com.alibaba.dashscope.legacy_2x;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.legacy_2x.ServerSentEvent.DataWithMediaType;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.jetty.util.AtomicBiInteger;

public class TestUtils {
  private static final MediaType EVENT_STREAM = MediaType.parse("text/event-stream");
  private static final MediaType APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");

  public static MockResponse createStreamMockResponse(List<String> responseBodies, int statusCode) {
    StringBuilder sseDataBuilder = new StringBuilder();
    responseBodies.forEach(
        responseBody -> {
          ServerSentEvent.SseEventBuilder event = new ServerSentEvent.SseEventBuilder();
          event.data(responseBody);
          Set<DataWithMediaType> x = event.build();
          for (DataWithMediaType d : x) {
            sseDataBuilder.append(d.getData());
          }
        });
    // last done
    return new MockResponse()
        .setHeader("Content-Type", EVENT_STREAM)
        .setResponseCode(statusCode)
        .setChunkedBody(sseDataBuilder.toString(), 20);
  }

  public static MockResponse createStreamRunMockResponse(JsonArray responseEvents, int statusCode) {
    StringBuilder sseDataBuilder = new StringBuilder();
    AtomicBiInteger id = new AtomicBiInteger();
    responseEvents.forEach(
        responseMessage -> {
          ServerSentEvent.SseEventBuilder eventBuilder = new ServerSentEvent.SseEventBuilder();
          String eventName = responseMessage.getAsJsonObject().get("event").getAsString();
          eventBuilder.name(eventName);
          eventBuilder.id(String.valueOf(id.getAndIncrement()));
          if (eventName.equals("done")) {
            eventBuilder.data(responseMessage.getAsJsonObject().get("data").getAsString());
          } else {
            eventBuilder.data(
                JsonUtils.toJson(responseMessage.getAsJsonObject().get("data").getAsJsonObject()));
          }
          Set<DataWithMediaType> x = eventBuilder.build();
          for (DataWithMediaType d : x) {
            sseDataBuilder.append(d.getData());
          }
        });
    // last done
    return new MockResponse()
        .setHeader("Content-Type", EVENT_STREAM)
        .setResponseCode(statusCode)
        .setChunkedBody(sseDataBuilder.toString(), 20);
  }

  public static MockResponse createMockResponse(String responseBody, int statusCode) {
    return new MockResponse()
        .setHeader("Content-Type", APPLICATION_JSON)
        .setResponseCode(statusCode)
        .setBody(responseBody);
  }

  public static MockResponse createMockResponse(
      String responseBody, int statusCode, Map<String, String> headers) {
    MockResponse rsp =
        new MockResponse()
            .setHeader("Content-Type", APPLICATION_JSON)
            .setResponseCode(statusCode)
            .setBody(responseBody);
    for (Entry<String, String> header : headers.entrySet()) {
      rsp.addHeader(header.getKey(), header.getValue());
    }
    return rsp;
  }

  public static void verifyRequest(
      RecordedRequest requestServer,
      Object requestObject,
      Class<?> classOfRequest,
      String path,
      String method) {
    assertEquals(requestServer.getPath(), path);
    assertEquals(requestServer.getMethod(), method);
    String body = requestServer.getBody().readUtf8();
    JsonObject requestObjectServer = JsonUtils.parse(body);
    Field[] declaredFields = classOfRequest.getDeclaredFields();
    for (Field field : declaredFields) {
      field.setAccessible(true);
      // field.getType()
      try {
        // Class<?> clazz = field.getType();
        Object value = field.get(requestObject);
        if (value != null) {
          JsonElement obj = JsonUtils.toJsonElement(value);
          JsonElement obj1 = requestObjectServer.get(field.getName());
          SerializedName serializeName = field.getAnnotation(SerializedName.class);
          if (serializeName != null) {
            obj1 = requestObjectServer.get(serializeName.value());
          }
          // assertEquals(obj, obj1);
          boolean isEqual = jsonElementsEqual(obj, obj1, true);
          assertTrue(isEqual);
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
  }

  public static <T> T convertObjectToType(Object obj, Class<T> clazz) {
    if (clazz.isInstance(obj)) {
      return clazz.cast(obj);
    }
    throw new IllegalArgumentException("The provided object is not of type " + clazz.getName());
  }

  /**
   * check if the json element is empty if object is empty, array is empty or string is empty,
   * return True.
   *
   * @param element
   * @return
   */
  public static boolean isJsonElementEmpty(JsonElement element) {
    if (element.isJsonObject()) {
      JsonObject o2 = element.getAsJsonObject();
      if (o2.entrySet().size() == 0) {
        return true;
      }
      return false;
    } else if (element.isJsonArray()) {
      JsonArray a2 = element.getAsJsonArray();
      if (a2.size() == 0) {
        return true;
      }
      return false;
    } else if (element.isJsonPrimitive()) {
      JsonPrimitive p2 = element.getAsJsonPrimitive();
      if (p2.isString()) {
        String s2 = p2.getAsString();
        if (s2.equals("")) {
          return true;
        } else {
          return false;
        }
      }
      return false;
    } else {
      return false;
    }
  }

  /**
   * Check if the json element is equal
   *
   * @param left
   * @param right
   * @param ignoreEmpty: null and empty object, list, string is equal if true.
   * @return
   */
  public static boolean jsonElementsEqual(
      JsonElement left, JsonElement right, boolean ignoreEmpty) {
    if (left == right) return true;

    if (ignoreEmpty) {
      if (left == null) {
        return isJsonElementEmpty(right);
      }
      if (right == null) {
        return isJsonElementEmpty(left);
      }
    }
    // If only one value is null, they are not equal
    if (left == null || right == null) return false;

    // Check that the element types match
    if (left.getClass() != right.getClass()) return false;

    // Compare the concrete JsonElement types
    if (left.isJsonObject()) {
      JsonObject objectLeft = left.getAsJsonObject();
      JsonObject objectRight = right.getAsJsonObject();

      // Check that the key sets have equal sizes
      if (objectLeft.size() != objectRight.size()) return false;

      // Check each key-value pair
      for (String key : objectLeft.keySet()) {
        if (!objectRight.has(key)) return false;
        if (!jsonElementsEqual(objectLeft.get(key), objectRight.get(key), ignoreEmpty))
          return false;
      }
      return true;
    } else if (left.isJsonArray()) {
      JsonArray arrayLeft = left.getAsJsonArray();
      JsonArray arrayRight = right.getAsJsonArray();

      // Check that the array lengths match
      if (arrayLeft.size() != arrayRight.size()) return false;

      // Recursively check each element
      for (int i = 0; i < arrayLeft.size(); i++) {
        if (!jsonElementsEqual(arrayLeft.get(i), arrayRight.get(i), ignoreEmpty)) return false;
      }
      return true;
    } else if (left.isJsonPrimitive()) {
      // Use equals directly for JsonPrimitive values
      JsonPrimitive primitiveLeft = left.getAsJsonPrimitive();
      JsonPrimitive primitiveRight = right.getAsJsonPrimitive();
      return primitiveLeft.equals(primitiveRight);
    } else if (left.isJsonNull()) {
      // All JsonNull values are considered equal
      return true;
    }

    // Return false if none of the above conditions match
    return false;
  }
}
