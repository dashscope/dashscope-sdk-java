// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.translation;

public enum TranslationRecognizerState {
  IDLE("idle"),
  SPEECH_TRANSLATION_STARTED("speech_translation_started"),
  ;
  private final String value;

  private TranslationRecognizerState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
