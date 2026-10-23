// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static com.alibaba.dashscope.utils.ApiKeywords.HISTORY;
import static com.alibaba.dashscope.utils.ApiKeywords.PROMPT;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class HalfDuplexTestParam extends HalfDuplexServiceParam {
  /** The input prompt. */
  @Getter private String prompt;

  @Getter private JsonArray history;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, getPrompt());
    if (history != null && history.size() > 0) {
      jsonObject.add(HISTORY, getHistory());
    }
    return jsonObject;
  }

  /**
   * Get the websocket binary data, only for websocket binary input data.
   *
   * @return
   */
  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, getInput());
    if (getParameters() != null) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(getParameters()));
    }
    if (getResources() != null) {
      requestObject.add(ApiKeywords.RESOURCES, (JsonObject) getResources());
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {}
}
