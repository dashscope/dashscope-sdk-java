// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import java.util.concurrent.Executor;

/**
 * Async {@code audio} resource group (audio design §1): only asr batch transcription rides the
 * async client — ws duplex surfaces (recognize/tts/realtime) stay sync-only.
 */
public final class AudioAsyncService {
  private final AsrAsyncService asr;

  public AudioAsyncService(AudioService sync, Executor executor) {
    this.asr = new AsrAsyncService(sync.asr(), executor);
  }

  public AsrAsyncService asr() {
    return asr;
  }
}
