// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.multimodal;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.alibaba.dashscope.types.common.Usage;
import com.alibaba.dashscope.types.completions.SearchInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * One multimodal streaming frame. When client-side merge applies (incremental_output=false
 * semantics for models without server support), frames arrive pre-merged into cumulative form.
 */
public class MultimodalChatChunk extends ResponseBase {
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

  /** First choice; null when the chunk carries no choices. */
  public MultimodalChoice firstChoice() {
    return choices == null || choices.isEmpty() ? null : choices.get(0);
  }
}
