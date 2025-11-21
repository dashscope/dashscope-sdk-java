package com.alibaba.dashscope.audio.qwen_asr;

import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@SuperBuilder
public class QwenTranscriptionQueryParam {
  private String taskId;

  private String apiKey;

  private Map<String, String> headers;

  public Map<String, String> getCustomHeaders() {
    return headers;
  }

  public static QwenTranscriptionQueryParam FromTranscriptionParam(
          QwenTranscriptionParam param, String taskId) {
    return QwenTranscriptionQueryParam.builder()
        .apiKey(param.getApiKey())
        .taskId(taskId)
        .headers(param.getHeaders())
        .build();
  }
}
