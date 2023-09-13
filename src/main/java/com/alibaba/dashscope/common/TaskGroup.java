// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum TaskGroup {
  AIGC("aigc"),
  EMBEDDINGS("embeddings"),
  AUDIO("audio"),
  NLP("nlp"),
  ;

  private final String value;

  private TaskGroup(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
