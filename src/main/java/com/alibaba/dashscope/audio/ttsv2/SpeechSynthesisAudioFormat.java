package com.alibaba.dashscope.audio.ttsv2;

public enum SpeechSynthesisAudioFormat {
  DEFAULT("Default", 0, "0", "0"),
  WAV_8000HZ_MONO_16BIT("wav", 8000, "mono", "16bit"),
  WAV_16000HZ_MONO_16BIT("wav", 16000, "mono", "16bit"),
  WAV_22050HZ_MONO_16BIT("wav", 22050, "mono", "16bit"),
  WAV_24000HZ_MONO_16BIT("wav", 24000, "mono", "16bit"),
  WAV_44100HZ_MONO_16BIT("wav", 44100, "mono", "16bit"),
  WAV_48000HZ_MONO_16BIT("wav", 48000, "mono", "16bit"),
  MP3_8000HZ_MONO_128KBPS("mp3", 8000, "mono", "128kbps"),
  MP3_16000HZ_MONO_128KBPS("mp3", 16000, "mono", "128kbps"),
  MP3_22050HZ_MONO_256KBPS("mp3", 22050, "mono", "256kbps"),
  MP3_24000HZ_MONO_256KBPS("mp3", 24000, "mono", "256kbps"),
  MP3_44100HZ_MONO_256KBPS("mp3", 44100, "mono", "256kbps"),
  MP3_48000HZ_MONO_256KBPS("mp3", 48000, "mono", "256kbps"),
  PCM_8000HZ_MONO_16BIT("pcm", 8000, "mono", "16bit"),
  PCM_16000HZ_MONO_16BIT("pcm", 16000, "mono", "16bit"),
  PCM_22050HZ_MONO_16BIT("pcm", 22050, "mono", "16bit"),
  PCM_24000HZ_MONO_16BIT("pcm", 24000, "mono", "16bit"),
  PCM_44100HZ_MONO_16BIT("pcm", 44100, "mono", "16bit"),
  PCM_48000HZ_MONO_16BIT("pcm", 48000, "mono", "16bit");
  private final String format;
  private final int sampleRate;
  private final String channels;
  private final String bitRate;

  // Constructor
  SpeechSynthesisAudioFormat(String format, int sampleRate, String channels, String bitRate) {
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
