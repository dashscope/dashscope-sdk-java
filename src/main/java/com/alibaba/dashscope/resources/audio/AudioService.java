// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.transport.HttpTransport;

/** {@code audio} resource group (audio design §1): asr / tts / realtime + management faces. */
public final class AudioService {
  private final AsrService asr;
  private final TtsService tts;
  private final RealtimeService realtime;

  public AudioService(HttpTransport transport) {
    this.asr = new AsrService(transport);
    this.tts = new TtsService(transport);
    this.realtime = new RealtimeService(transport);
  }

  public AsrService asr() {
    return asr;
  }

  public TtsService tts() {
    return tts;
  }

  public RealtimeService realtime() {
    return realtime;
  }
}
