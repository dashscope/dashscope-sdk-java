// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.rerank;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The original document text of a rerank result (present when return_documents=true). */
public class RerankDocument {
  @JsonProperty("text")
  private String text;

  public String getText() {
    return text;
  }
}
