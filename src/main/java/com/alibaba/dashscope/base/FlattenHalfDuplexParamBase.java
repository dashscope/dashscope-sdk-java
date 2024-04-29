package com.alibaba.dashscope.base;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class FlattenHalfDuplexParamBase extends HalfDuplexParamBase {
  @Override
  public String getModel() {
    // not use
    throw new UnsupportedOperationException("Unimplemented method 'getModel'");
  }

  @Override
  public Map<String, Object> getParameters() {
    return parameters;
  }

  @Override
  public Object getInput() {
    // not use
    throw new UnsupportedOperationException("Unimplemented method 'getInput'");
  }

  @Override
  public Object getResources() {
    // not use
    throw new UnsupportedOperationException("Unimplemented method 'getResources'");
  }

  @Override
  public ByteBuffer getBinaryData() {
    // not use
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {
    // not use
    throw new UnsupportedOperationException("Unimplemented method 'validate'");
  }

  @Override
  public Map<String, String> getHeaders() {
    Map<String, String> res = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().toString();
      res.put(key, value);
    }
    return res;
  }

  public void putHeader(String key, String value) {
    if (headers.size() == 0) {
      headers = new HashMap<String, Object>();
    } else {
      HashMap<String, Object> newHeaders = new HashMap<>();
      newHeaders.putAll(headers);
      headers = newHeaders;
    }
    headers.put(key, value);
  }

  protected void addExtraBody(JsonObject requestObject) {
    if (parameters != null && !parameters.isEmpty()) {
      for (Entry<String, Object> item : parameters.entrySet()) {
        JsonElement element = JsonUtils.toJsonElement(item.getValue());
        if (element.isJsonPrimitive()) {
          JsonPrimitive jsonPrimitive = (JsonPrimitive) element;
          if (jsonPrimitive.isString()) {
            requestObject.addProperty(item.getKey(), jsonPrimitive.getAsString());
          } else if (jsonPrimitive.isNumber()) {
            requestObject.addProperty(item.getKey(), jsonPrimitive.getAsNumber());
          } else if (jsonPrimitive.isBoolean()) {
            requestObject.addProperty(item.getKey(), jsonPrimitive.getAsBoolean());
          }
        } else if (element.isJsonNull()) {
          requestObject.add(item.getKey(), element.getAsJsonNull());
        } else if (element.isJsonArray()) {
          requestObject.add(item.getKey(), JsonUtils.toJsonArray(item.getValue()));
        } else {
          requestObject.add(item.getKey(), JsonUtils.toJsonObject(item.getValue()));
        }
      }
    }
  }
}
