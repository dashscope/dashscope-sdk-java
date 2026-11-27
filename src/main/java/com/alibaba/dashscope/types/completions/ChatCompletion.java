// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.completions;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.alibaba.dashscope.types.common.Usage;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Non-streaming chat completion. Envelope fields live on {@link ResponseBase}. */
public class ChatCompletion extends ResponseBase {
  @JsonProperty("model")
  private String model;

  @JsonProperty("choices")
  private List<Choice> choices;

  @JsonProperty("usage")
  private Usage usage;

  @JsonProperty("search_info")
  private SearchInfo searchInfo;

  public String getModel() {
    return model;
  }

  public List<Choice> getChoices() {
    return choices;
  }

  public Usage getUsage() {
    return usage;
  }

  public SearchInfo getSearchInfo() {
    return searchInfo;
  }

  /** First choice's message content; empty string when absent. */
  public String outputText() {
    if (choices == null || choices.isEmpty() || choices.get(0).getMessage() == null) {
      return "";
    }
    String content = choices.get(0).getMessage().getContent();
    return content == null ? "" : content;
  }
}
