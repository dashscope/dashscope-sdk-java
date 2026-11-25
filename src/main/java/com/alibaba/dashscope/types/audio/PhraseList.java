// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/** Page of compiled phrase sets ({@code GET /fine-tunes/outputs}). */
public final class PhraseList extends ResponseBase {
  @JsonProperty("page_no")
  private int pageNo = 1;

  @JsonProperty("page_size")
  private int pageSize = 10;

  @JsonProperty("total_count")
  @JsonAlias("total")
  private int totalCount;

  @JsonProperty("phrases")
  @JsonAlias({"jobs", "outputs", "finetuned_outputs"})
  private List<Phrase> phrases = new ArrayList<>();

  public int getPageNo() {
    return pageNo;
  }

  public int getPageSize() {
    return pageSize;
  }

  public int getTotalCount() {
    return totalCount;
  }

  public List<Phrase> getPhrases() {
    return phrases;
  }
}
