// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x;

import com.alibaba.dashscope.legacy_2x.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.utils.ApiKeywords;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class HalfDuplexBinaryParam extends HalfDuplexServiceParam {
  ByteBuffer data;

  @Override
  public JsonObject getInput() {
    return null;
  }

  /**
   * Get the websocket binary data, only for websocket binary input data.
   *
   * @return
   */
  @Override
  public ByteBuffer getBinaryData() {
    return data;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, getInput());
    if (getParameters() != null) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(getParameters()));
    }
    return requestObject;
  }

  @Override
  public void validate() throws InputRequiredException {}
}
