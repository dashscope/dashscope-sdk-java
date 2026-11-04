// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.audio.tts;

@Deprecated
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
