// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class MultiModalEmbeddingParam extends HalfDuplexServiceParam {
  @NonNull private List<MultiModalEmbeddingItemBase> contents;

  public List<MultiModalEmbeddingItemBase> getContent() {
    return contents;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, getInput());
    if (parameters != null && !parameters.isEmpty()) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(getParameters()));
    }
    return requestObject;
  }

  @Override
  public JsonObject getInput() {
    JsonObject input = new JsonObject();
    input.add("contents", JsonUtils.toJsonArray(contents));
    return input;
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {
    if (contents.isEmpty()) {
      throw new InputRequiredException("contents must not empty");
    }
  }
}
