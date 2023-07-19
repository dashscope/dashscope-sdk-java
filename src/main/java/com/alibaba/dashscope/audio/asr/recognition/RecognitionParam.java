package com.alibaba.dashscope.audio.asr.recognition;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
public class RecognitionParam {

  @Builder.Default private boolean disfluencyRemovalEnabled = false;

  private String apiKey;

  @NonNull private String model;

  @NonNull private Integer sampleRate;

  @NonNull private String format;
}
