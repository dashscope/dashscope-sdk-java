// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

/**
 * TTS model routing (audio design §8). Pinned by unit tests:
 *
 * <ul>
 *   <li>{@code qwen-audio-*} → SpeechSynthesizer path
 *   <li>{@code qwen*} (qwen-tts, qwen3-tts-flash) → multimodal-generation path
 *   <li>everything else (sambert/cosyvoice) → SpeechSynthesizer path
 *   <li>ws duplex ({@code tts().connect}) is the cosyvoice family; qwen-tts family belongs on HTTP
 * </ul>
 */
final class AudioModelRouter {
  static final String SPEECH_SYNTHESIZER_PATH = "/services/audio/tts/SpeechSynthesizer";
  static final String QWEN_TTS_PATH = "/services/aigc/multimodal-generation/generation";

  private AudioModelRouter() {}

  static String httpPath(String model) {
    String m = model == null ? "" : model.toLowerCase();
    if (m.startsWith("qwen-audio-")) {
      return SPEECH_SYNTHESIZER_PATH;
    }
    if (m.startsWith("qwen")) {
      return QWEN_TTS_PATH;
    }
    return SPEECH_SYNTHESIZER_PATH;
  }
}
