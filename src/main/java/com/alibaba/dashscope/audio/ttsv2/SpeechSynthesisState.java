package com.alibaba.dashscope.audio.ttsv2;

public enum SpeechSynthesisState {
  IDLE("idle"),
  TTS_STARTED("stream_input_tts_started"),
  ;
  private final String value;

  private SpeechSynthesisState(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
