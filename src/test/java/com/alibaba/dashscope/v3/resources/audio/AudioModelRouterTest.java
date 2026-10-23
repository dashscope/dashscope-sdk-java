// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Pins the TTS model routing table (audio design §8). */
class AudioModelRouterTest {

  @Test
  void httpPathRouting() {
    assertEquals(
        AudioModelRouter.SPEECH_SYNTHESIZER_PATH, AudioModelRouter.httpPath("qwen-audio-tts"));
    assertEquals(AudioModelRouter.QWEN_TTS_PATH, AudioModelRouter.httpPath("qwen-tts"));
    assertEquals(AudioModelRouter.QWEN_TTS_PATH, AudioModelRouter.httpPath("qwen3-tts-flash"));
    assertEquals(AudioModelRouter.SPEECH_SYNTHESIZER_PATH, AudioModelRouter.httpPath("sambert-v1"));
    assertEquals(
        AudioModelRouter.SPEECH_SYNTHESIZER_PATH, AudioModelRouter.httpPath("cosyvoice-v2"));
    assertEquals(AudioModelRouter.SPEECH_SYNTHESIZER_PATH, AudioModelRouter.httpPath(null));
  }

  @Test
  void preferredRouteRouting() {
    assertEquals(
        AudioModelRouter.PreferredRoute.CONNECT, AudioModelRouter.preferredRoute("cosyvoice-v2"));
    assertEquals(
        AudioModelRouter.PreferredRoute.CONNECT,
        AudioModelRouter.preferredRoute("CosyVoice-v3-flash"));
    assertEquals(
        AudioModelRouter.PreferredRoute.HTTP, AudioModelRouter.preferredRoute("qwen3-tts-flash"));
    assertEquals(AudioModelRouter.PreferredRoute.HTTP, AudioModelRouter.preferredRoute(null));
  }
}
