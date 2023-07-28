// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition;

public enum RecognitionState {
  IDLE("idle"),
  RECOGNITION_STARTED("recognition_started"),
  ;
  private final String value;

  private RecognitionState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
