// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.tts;

public enum SpeechSynthesisTextType {
  PLAIN_TEXT("PlainText"),
  SSML("SSML"),
  ;

  private final String value;

  private SpeechSynthesisTextType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
