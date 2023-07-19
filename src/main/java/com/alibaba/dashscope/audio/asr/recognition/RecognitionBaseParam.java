package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.common.*;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public abstract class RecognitionBaseParam extends Param {
  @Override
  public String url() {
    return String.format(
        "/services/%s/%s/%s",
        TaskGroup.AUDIO.getValue(), Task.ASR.getValue(), Function.RECOGNITION.getValue());
  }

  @Override
  public Class<? extends Result> resultType() {
    return RecognitionResult.class;
  }
}
