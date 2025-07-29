package com.alibaba.dashscope.multimodal.tingwu;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.nio.ByteBuffer;
import java.util.Map;

@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class TingWuParam extends HalfDuplexServiceParam {

  private Map<String, Object> input;
  @Override
  public Map<String, Object> getInput() {
    return input;
  }

  /**
   * Get the websocket binary data, only for websocket binary input data.
   **/
  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, JsonUtils.toJsonObject(getInput()));
    Map<String, Object> params = getParameters();
    if (params != null && !params.isEmpty()) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(params));
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (getInput() == null) {
      throw new InputRequiredException("Input is required");
    }
  }
}
