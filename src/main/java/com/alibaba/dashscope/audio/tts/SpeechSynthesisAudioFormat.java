// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.tts;

public enum SpeechSynthesisAudioFormat {
  PCM("pcm"),
  WAV("wav"),
  MP3("mp3"),
  ;

  private final String value;

  private SpeechSynthesisAudioFormat(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
