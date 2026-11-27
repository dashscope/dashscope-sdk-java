package com.alibaba.dashscope.legacy_2x.audio.asr.vocabulary;

@Deprecated
public enum VocabularyOperationType {
  CREATE("create_vocabulary"),
  LIST("list_vocabulary"),
  QUERY("query_vocabulary"),
  UPDATE("update_vocabulary"),
  DELETE("delete_vocabulary");
  private final String value;

  private VocabularyOperationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
