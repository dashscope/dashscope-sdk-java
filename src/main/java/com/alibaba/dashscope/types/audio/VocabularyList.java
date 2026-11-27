// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.types.audio;

import com.alibaba.dashscope.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Page of hot-word tables; items keep their wire shape (the service adds fields over time). */
public final class VocabularyList extends ResponseBase {
  @JsonProperty("vocabularies")
  @JsonAlias("vocabulary_list")
  private List<Map<String, Object>> vocabularies = new ArrayList<>();

  public List<Map<String, Object>> getVocabularies() {
    return vocabularies;
  }
}
