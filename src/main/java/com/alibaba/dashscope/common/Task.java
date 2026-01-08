// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import lombok.Getter;

@Getter
public enum Task {
  TEXT_GENERATION("text-generation"),
  CODE_GENERATION("code-generation"),
  MULTIMODAL_GENERATION("multimodal-generation"),
  IMAGE_GENERATION("image-generation"),
  IMAGE_SYNTHESIS("text2image"),
  TEXT_EMBEDDING("text-embedding"),
  MULTIMODAL_EMBEDDING("multimodal-embedding"),
  CHAT("chat"),
  TEXT_TO_SPEECH("tts"),
  ASR("asr"),
  NLU("nlu"),
  ;

  private final String value;

  Task(String value) {
    this.value = value;
  }

}
