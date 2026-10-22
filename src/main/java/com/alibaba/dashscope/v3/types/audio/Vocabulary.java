// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.types.audio;

import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/** One hot-word table; {@code vocabularies().create}/{@code get} return it. */
public final class Vocabulary extends ResponseBase {
  @JsonProperty("vocabulary_id")
  private String vocabularyId;

  @JsonProperty("vocabulary")
  private List<Map<String, Object>> vocabulary;

  public String getVocabularyId() {
    return vocabularyId;
  }

  public List<Map<String, Object>> getVocabulary() {
    return vocabulary;
  }
}
