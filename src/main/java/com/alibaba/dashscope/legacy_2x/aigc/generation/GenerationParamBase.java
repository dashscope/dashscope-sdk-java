// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.aigc.generation;

import static com.alibaba.dashscope.legacy_2x.utils.ApiKeywords.HISTORY;
import static com.alibaba.dashscope.legacy_2x.utils.ApiKeywords.PROMPT;

import com.alibaba.dashscope.legacy_2x.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.legacy_2x.common.History;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.utils.ApiKeywords;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@Deprecated
public abstract class GenerationParamBase extends HalfDuplexServiceParam {
  /** The input prompt. */
  private String prompt;

  private List<History> history;

  @Override
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(PROMPT, getPrompt());
    if (history != null && !history.isEmpty()) {
      JsonArray ar = JsonUtils.toJsonElement(history).getAsJsonArray();
      jsonObject.add(HISTORY, ar);
    }
    return jsonObject;
  }

  /**
   * Get the websocket binary data, only for websocket binary input data.
   *
   * @return Generation param has no binary data.
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
    Map<String, Object> params = getParameters();
    if (params != null && !params.isEmpty()) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(params));
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {}
}
