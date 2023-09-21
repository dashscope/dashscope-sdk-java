package com.alibaba.dashscope.aigc.codegeneration;

import com.alibaba.dashscope.aigc.codegeneration.models.MessageParamBase;
import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class CodeGenerationParam extends HalfDuplexServiceParam {

  private String scene;

  private List<MessageParamBase> message;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(ApiKeywords.SCENE, scene);
    jsonObject.add(ApiKeywords.MESSAGE, JsonUtils.toJsonArray(message));
    return jsonObject;
  }

  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, getInput());
    Map<String, Object> params = getParameters();
    if (params == null || params.isEmpty()) {
      params = new HashMap<>();
      params.put("n", 1);
    }
    requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(params));
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {
    if ((getScene() == null || getScene().isEmpty())
        || (getMessage() == null || getMessage().isEmpty())) {
      throw new InputRequiredException("scene and message must not all null");
    }
  }
}
