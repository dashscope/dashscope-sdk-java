// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.rerank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parameters for {@code rerank.create}. Wire shape: {@code {model, input:{query, documents},
 * parameters:{...}}}.
 */
public final class RerankCreateParams {
  private final String model;
  private final String query;
  private final List<String> documents;
  private final Integer topN;
  private final Boolean returnDocuments;
  private final String instruct;
  private final Map<String, Object> parameters;

  private RerankCreateParams(Builder b) {
    this.model = b.model;
    this.query = b.query;
    this.documents = b.documents;
    this.topN = b.topN;
    this.returnDocuments = b.returnDocuments;
    this.instruct = b.instruct;
    this.parameters = b.parameters;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getModel() {
    return model;
  }

  public String getQuery() {
    return query;
  }

  public List<String> getDocuments() {
    return documents;
  }

  public Integer getTopN() {
    return topN;
  }

  public Boolean getReturnDocuments() {
    return returnDocuments;
  }

  public String getInstruct() {
    return instruct;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public static final class Builder {
    private String model;
    private String query;
    private List<String> documents;
    private Integer topN;
    private Boolean returnDocuments;
    private String instruct;
    private Map<String, Object> parameters;

    private Builder() {}

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder query(String query) {
      this.query = query;
      return this;
    }

    public Builder documents(List<String> documents) {
      this.documents = documents == null ? null : new ArrayList<>(documents);
      return this;
    }

    public Builder topN(Integer topN) {
      this.topN = topN;
      return this;
    }

    public Builder returnDocuments(Boolean returnDocuments) {
      this.returnDocuments = returnDocuments;
      return this;
    }

    public Builder instruct(String instruct) {
      this.instruct = instruct;
      return this;
    }

    public Builder parameters(Map<String, Object> parameters) {
      this.parameters = parameters;
      return this;
    }

    public RerankCreateParams build() {
      if (model == null || model.isEmpty()) {
        throw new IllegalArgumentException("model is required");
      }
      if (query == null || query.isEmpty()) {
        throw new IllegalArgumentException("query is required");
      }
      if (documents == null || documents.isEmpty()) {
        throw new IllegalArgumentException("documents must not be empty");
      }
      return new RerankCreateParams(this);
    }
  }
}
