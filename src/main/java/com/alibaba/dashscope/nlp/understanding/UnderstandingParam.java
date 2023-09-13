package com.alibaba.dashscope.nlp.understanding;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class UnderstandingParam extends HalfDuplexServiceParam {

  private String sentence;

  private String labels;

  private String task;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(ApiKeywords.SENTENCE, sentence);
    jsonObject.addProperty(ApiKeywords.LABELS, labels);
    if (task != null && !task.isEmpty()) {
      jsonObject.addProperty(ApiKeywords.TASK, task);
    }
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
    if (params != null && !params.isEmpty()) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(params));
    } else {
      requestObject.add(ApiKeywords.PARAMETERS, new JsonObject());
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {}
}
