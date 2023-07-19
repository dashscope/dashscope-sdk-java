package com.alibaba.dashscope.audio.asr.transcription;

import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.common.WebSocketEventType;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.*;
import com.google.common.collect.Maps;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Transcription implements BaseTranscription {

  @Setter private long responseTime = 30;

  @Override
  public TranscriptionResult asyncCall(TranscriptionParam param) throws ApiException {
    try {
      param.setMode(StreamingMode.NONE);
      Map<String, String> exHeaders = Maps.newHashMap();
      exHeaders.put("X-DashScope-Async", "enable");
      return (TranscriptionResult)
          ServiceFacility.call(Protocol.HTTP, exHeaders, param, HttpMethod.POST, responseTime);
    } catch (Exception e) {
      throw e instanceof ApiException ? (ApiException) e : new ApiException(e);
    }
  }

  @Override
  public TranscriptionResult wait(TranscriptionQueryParam queryParam) {
    try {
      SubmitParam param =
          SubmitParam.builder()
              .taskId(queryParam.getTaskId())
              .apiKey(queryParam.getApiKey())
              .build();
      return (TranscriptionResult)
          ServiceFacility.wait(param, TranscriptionResult.class, responseTime);
    } catch (Exception e) {
      throw e instanceof ApiException ? (ApiException) e : new ApiException(e);
    }
  }

  @Override
  public TranscriptionResult fetch(TranscriptionQueryParam queryParam) {
    try {
      SubmitParam param =
          SubmitParam.builder()
              .mode(StreamingMode.NONE)
              .taskId(queryParam.getTaskId())
              .apiKey(queryParam.getApiKey())
              .build();
      Map<String, String> headers = ServiceFacility.prepareHeaders(Protocol.HTTP, param);
      String url = ServiceFacility.prepareUrl(Protocol.HTTP, param);
      Response response =
          ServiceFacility.streamingNone(
              Protocol.HTTP,
              url,
              headers,
              ServiceFacility.prepareRequest(Protocol.HTTP, param, WebSocketEventType.RUN_TASK),
              HttpMethod.GET,
              responseTime);
      return (TranscriptionResult)
          ServiceFacility.prepareResult(Protocol.HTTP, queryParam, response);
    } catch (Exception e) {
      throw e instanceof ApiException ? (ApiException) e : new ApiException(e);
    }
  }
}
