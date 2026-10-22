// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.completions;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** One streaming chunk of a chat completion. */
public class ChatCompletionChunk extends ResponseBase {
  @JsonProperty("model")
  private String model;

  @JsonProperty("choices")
  private List<StreamChoice> choices;

  @JsonProperty("usage")
  private Usage usage;

  @JsonProperty("search_info")
  private SearchInfo searchInfo;

  public String getModel() {
    return model;
  }

  public List<StreamChoice> getChoices() {
    return choices;
  }

  public Usage getUsage() {
    return usage;
  }

  public SearchInfo getSearchInfo() {
    return searchInfo;
  }

  /** First choice's delta; null when the chunk carries no choices. */
  public Delta delta() {
    if (choices == null || choices.isEmpty()) {
      return null;
    }
    return choices.get(0).getDelta();
  }
}
