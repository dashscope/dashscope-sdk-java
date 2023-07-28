// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.embeddings;

import com.alibaba.dashscope.base.HalfDuplexServiceParam;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class TextEmbeddingParam extends HalfDuplexServiceParam {
  public enum TextType {
    QUERY("query"),
    DOCUMENT("document"),
    ;

    private final String value;

    private TextType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  @Singular private List<String> texts;

  // query or document
  /*After the text is converted into a vector, it can be applied to downstream tasks such as retrieval,
  clustering, and classification. For asymmetric tasks such as retrieval, in order to achieve better
  retrieval results, it is recommended to distinguish between query text (query) and bottom database
  text (document) types, clustering Symmetric tasks such as , classification, etc. do not need to be
  specially specified, and the system default value "document" can be used */
  private TextType textType;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    if (textType != null) {
      params.put("text_type", textType.getValue());
    }
    if (parameters != null && !parameters.isEmpty()) {
      params.putAll(parameters);
    }
    return params;
  }

  @Override
  public JsonObject getHttpBody() {
    JsonObject requestObject = new JsonObject();
    requestObject.addProperty(ApiKeywords.MODEL, getModel());
    requestObject.add(ApiKeywords.INPUT, getInput());
    if (getParameters() != null && !getParameters().isEmpty()) {
      requestObject.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(getParameters()));
    }
    return requestObject;
  }

  @Override
  public JsonObject getInput() {
    JsonObject input = new JsonObject();
    input.add("texts", JsonUtils.toJsonArray(texts));
    return input;
  }

  @Override
  public ByteBuffer getBinaryData() {
    throw new UnsupportedOperationException("Unimplemented method 'getBinaryData'");
  }

  @Override
  public void validate() throws InputRequiredException {
    if (texts.isEmpty()) {
      throw new InputRequiredException("texts must not empty");
    }
  }
}
