// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

/**
 * Realtime protocol dialects, routed by model name prefix (audio design §8). The dialect selects
 * the {@code session.update} field mapping and session-lifecycle capabilities.
 */
enum RealtimeDialect {
  OMNI,
  OMNI_V38,
  QWEN_AUDIO,
  LIVETRANSLATE,
  LIVETRANSLATE_V38;

  static RealtimeDialect fromModel(String model) {
    if (model == null) {
      return OMNI;
    }
    if (model.startsWith("qwen3.8-omni-")) {
      return OMNI_V38;
    }
    if (model.startsWith("qwen-audio-")) {
      return QWEN_AUDIO;
    }
    if (model.startsWith("qwen3.8-livetranslate-")) {
      return LIVETRANSLATE_V38;
    }
    if (model.startsWith("qwen3-livetranslate-") || model.startsWith("qwen3.5-livetranslate-")) {
      return LIVETRANSLATE;
    }
    return OMNI;
  }
}
