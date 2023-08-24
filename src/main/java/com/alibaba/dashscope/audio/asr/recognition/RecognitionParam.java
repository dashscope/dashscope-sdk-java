// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.asr.recognition;

import com.alibaba.dashscope.base.FullDuplexServiceParam;
import io.reactivex.Flowable;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Data
public class RecognitionParam extends FullDuplexServiceParam {

  @Builder.Default private boolean disfluencyRemovalEnabled = false;

  @NonNull private Integer sampleRate;

  @NonNull private String format;

  @Override
  public Map<String, Object> getParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put(RecognitionApiKeywords.FORMAT, format);
    params.put(RecognitionApiKeywords.SAMPLE_RATE, sampleRate);
    params.put(RecognitionApiKeywords.DISFLUENCY_REMOVAL_ENABLED, disfluencyRemovalEnabled);
    params.putAll(parameters);
    return params;
  }

  @Override
  public Flowable<Object> getStreamingData() {
    return null;
  }
}
