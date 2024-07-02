package com.alibaba.dashscope.audio.asr.transcription;

import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class TranscriptionQueryParam {
  private String taskId;

  private String apiKey;

  private Map<String, String> headers;

  public Map<String, String> getCustomHeaders() {
    return headers;
  }

  public static TranscriptionQueryParam FromTranscriptionParam(
      TranscriptionParam param, String taskId) {
    return TranscriptionQueryParam.builder()
        .apiKey(param.getApiKey())
        .taskId(taskId)
        .headers(param.getHeaders())
        .build();
  }
}
