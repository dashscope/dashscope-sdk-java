// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.asr.translation;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class TranslationRecognizerUsage {
  private Integer duration;
}
