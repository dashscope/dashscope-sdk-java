// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.asr.phrase;

@Deprecated
public enum AsrPhraseOperationType {
  CREATE("Create"),
  UPDATE("Update"),
  DELETE("Delete"),
  QUERY("Query"),
  LIST("List");

  private final String value;

  private AsrPhraseOperationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
