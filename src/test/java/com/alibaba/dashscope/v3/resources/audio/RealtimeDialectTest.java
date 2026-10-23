// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Pins the realtime dialect routing table (audio design §8). */
class RealtimeDialectTest {

  @Test
  void dialectFollowsModelPrefix() {
    assertEquals(
        RealtimeDialect.OMNI_V38, RealtimeDialect.fromModel("qwen3.8-omni-flash-realtime"));
    assertEquals(RealtimeDialect.QWEN_AUDIO, RealtimeDialect.fromModel("qwen-audio-asr-realtime"));
    assertEquals(
        RealtimeDialect.LIVETRANSLATE_V38,
        RealtimeDialect.fromModel("qwen3.8-livetranslate-flash-realtime"));
    assertEquals(
        RealtimeDialect.LIVETRANSLATE,
        RealtimeDialect.fromModel("qwen3-livetranslate-flash-realtime"));
    assertEquals(
        RealtimeDialect.LIVETRANSLATE,
        RealtimeDialect.fromModel("qwen3.5-livetranslate-flash-realtime"));
    assertEquals(RealtimeDialect.OMNI, RealtimeDialect.fromModel("qwen3-omni-flash-realtime"));
    assertEquals(RealtimeDialect.OMNI, RealtimeDialect.fromModel(null));
  }
}
