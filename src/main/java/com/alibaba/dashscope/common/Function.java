// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum Function {
  GENERATION("generation"),

  MULTIMODAL_CHAT("multimodal-chat"),

  SPEECH_SYNTHESIZER("SpeechSynthesizer"),

  TRANSCRIPTION("transcription"),

  RECOGNITION("recognition"),
  ;

  private final String value;

  private Function(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
