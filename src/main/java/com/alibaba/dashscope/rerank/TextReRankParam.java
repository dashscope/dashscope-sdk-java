// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.rerank;

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
import lombok.Singular;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
public class TextReRankParam extends HalfDuplexServiceParam {

  /** The query text for reranking. Maximum length is 4,000 tokens. */
  private String query;

  /** The list of candidate documents to be reranked. Maximum 500 documents. */
  @Singular private List<String> documents;

  /** 
   * The number of top documents to return. 
   * If not specified, returns all candidate documents.
   * If top_n is greater than the number of input documents, returns all documents.
   */
  private Integer topN;

  /** 
   * Whether to return the original document text in the results. 
   * Default is false.
   */
  private Boolean returnDocuments;

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
  public JsonObject getInput() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("query", query);
    jsonObject.add("documents", JsonUtils.toJsonArray(documents));
    return jsonObject;
  }

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    
    if (topN != null) {
      params.put("top_n", topN);
    }

    if (returnDocuments != null) {
      params.put("return_documents", returnDocuments);
    }
    
    params.putAll(parameters);
    return params;
  }

  @Override
  public ByteBuffer getBinaryData() {
    return null;
  }

  @Override
  public void validate() throws InputRequiredException {
    if (query == null || query.trim().isEmpty()) {
      throw new InputRequiredException("Query must not be null or empty!");
    }

    if (documents == null || documents.isEmpty()) {
      throw new InputRequiredException("Documents must not be null or empty!");
    }
  }
}