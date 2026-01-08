// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import lombok.Getter;

@Getter
public enum TaskGroup {
  AIGC("aigc"),
  EMBEDDINGS("embeddings"),
  AUDIO("audio"),
  NLP("nlp"),
  RERANK("rerank"),
  ;

  private final String value;

  TaskGroup(String value) {
    this.value = value;
  }

}
