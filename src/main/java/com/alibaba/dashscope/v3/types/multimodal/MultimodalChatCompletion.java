// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.multimodal;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.common.Usage;
import com.alibaba.dashscope.v3.types.completions.SearchInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Non-streaming multimodal completion. Envelope fields live on {@link ResponseBase}. */
public class MultimodalChatCompletion extends ResponseBase {
  @JsonProperty("model")
  private String model;

  @JsonProperty("choices")
  private List<MultimodalChoice> choices;

  @JsonProperty("usage")
  private Usage usage;

  @JsonProperty("search_info")
  private SearchInfo searchInfo;

  public String getModel() {
    return model;
  }

  public List<MultimodalChoice> getChoices() {
    return choices;
  }

  public Usage getUsage() {
    return usage;
  }

  public SearchInfo getSearchInfo() {
    return searchInfo;
  }

  /** First choice; null when absent. */
  public MultimodalChoice firstChoice() {
    return choices == null || choices.isEmpty() ? null : choices.get(0);
  }

  /** First choice's joined text content; empty string when absent. */
  public String outputText() {
    MultimodalChoice c = firstChoice();
    return c == null || c.getMessage() == null ? "" : c.getMessage().text();
  }
}
