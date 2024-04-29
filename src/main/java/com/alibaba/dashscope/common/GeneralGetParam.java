package com.alibaba.dashscope.common;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class GeneralGetParam extends HalfDuplexParamBase {
  @Override
  public String getModel() {
    throw new UnsupportedOperationException("Unimplemented method 'getModel'");
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    params.putAll(this.parameters);
    return params;
  }

  @Override
  public void validate() throws InputRequiredException {}

  @Override
  public Object getResources() {
    return null;
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

  @Override
  public JsonObject getHttpBody() {
    return null;
  }

  @Override
  public Object getInput() {
    throw new UnsupportedOperationException("Unimplemented method 'getInput'");
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }
}
