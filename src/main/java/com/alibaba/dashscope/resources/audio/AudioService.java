// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.audio;

import com.alibaba.dashscope.transport.HttpTransport;

/** {@code audio} resource group (audio design §1): asr / tts / realtime + management faces. */
public final class AudioService {
  private final AsrService asr;
  private final TtsService tts;
  private final RealtimeService realtime;
  private final VocabulariesService vocabularies;
  private final PhrasesService phrases;
  private final VoicesService voices;
  private final VoiceDesignService voiceDesign;

  public AudioService(HttpTransport transport) {
    this.asr = new AsrService(transport);
    this.tts = new TtsService(transport);
    this.realtime = new RealtimeService(transport);
    this.vocabularies = new VocabulariesService(transport);
    this.phrases = new PhrasesService(transport);
    this.voices = new VoicesService(transport);
    this.voiceDesign = new VoiceDesignService(transport);
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

  /** ASR hot-word tables: create/get/list/update/delete. */
  public VocabulariesService vocabularies() {
    return vocabularies;
  }

  /** Paraformer hot phrases: create/update compile via {@code /fine-tunes}. */
  public PhrasesService phrases() {
    return phrases;
  }

  /** Voice cloning: create/get/list/update/delete. */
  public VoicesService voices() {
    return voices;
  }

  /** Qwen voice design: create/get/list/delete. */
  public VoiceDesignService voiceDesign() {
    return voiceDesign;
  }
}
