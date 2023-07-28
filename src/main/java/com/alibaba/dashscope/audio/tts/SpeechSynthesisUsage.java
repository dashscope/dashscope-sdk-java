// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.tts;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class SpeechSynthesisUsage {
  private Integer characters;
}
