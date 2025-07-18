// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.qwen_tts_realtime;

import com.google.gson.annotations.SerializedName;

/** @author lengjiayi */
public enum QwenTtsRealtimeAudioFormat {
  @SerializedName("pcm24")
  PCM_24000HZ_MONO_16BIT("pcm", 24000, "mono", "16bit");
  private final String format;
  private final int sampleRate;
  private final String channels;
  private final String bitRate;

  // Constructor
  QwenTtsRealtimeAudioFormat(String format, int sampleRate, String channels, String bitRate) {
    this.format = format;
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.bitRate = bitRate;
  }

  // Getter methods
  public String getFormat() {
    return format;
  }

  public int getSampleRate() {
    return sampleRate;
  }

  public String getChannels() {
    return channels;
  }

  public String getBitRate() {
    return bitRate;
  }

  @Override
  public String toString() {
    return String.format(
        "%s with %dHz sample rate, %s channel, %s",
        format.toUpperCase(), sampleRate, channels, bitRate);
  }
}
