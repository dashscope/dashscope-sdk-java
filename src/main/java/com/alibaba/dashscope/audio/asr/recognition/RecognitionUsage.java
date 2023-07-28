// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.asr.recognition;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class RecognitionUsage {
  private Integer duration;
}
