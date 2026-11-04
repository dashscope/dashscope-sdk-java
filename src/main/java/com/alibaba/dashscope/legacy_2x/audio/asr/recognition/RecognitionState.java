// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.asr.recognition;

@Deprecated
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
