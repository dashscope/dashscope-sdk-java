package com.alibaba.dashscope.audio.ttsv2.enrollment;

public enum VoiceEnrollmentOperationType {
  CREATE("create_voice"),
  LIST("list_voice"),
  QUERY("query_voice"),
  UPDATE("update_voice"),
  DELETE("delete_voice");
  private final String value;

  private VoiceEnrollmentOperationType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
