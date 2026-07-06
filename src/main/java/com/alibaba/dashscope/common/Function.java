// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum Function {
  GENERATION("generation"),
  IMAGE_SYNTHESIS("image-synthesis"),
  TEXT_EMBEDDING("text-embedding"),
  MULTIMODAL_EMBEDDING("multimodal-embedding"),
  SPEECH_SYNTHESIZER("SpeechSynthesizer"),
  TRANSCRIPTION("transcription"),
  RECOGNITION("recognition"),
  SPEECH_TRANSLATION("recognition"),
  UNDERSTANDING("understanding"),
  ;

  private final String value;

  private Function(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
