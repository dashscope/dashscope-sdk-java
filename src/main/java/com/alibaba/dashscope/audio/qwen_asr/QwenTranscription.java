// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.audio.qwen_asr;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.common.Function;
import com.alibaba.dashscope.common.Task;
import com.alibaba.dashscope.common.TaskGroup;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class QwenTranscription {
  private final AsynchronousApi<QwenTranscriptionParam> asyncApi;
  private final ApiServiceOption createServiceOptions;
  private final String baseUrl;

  public QwenTranscription() {
    asyncApi = new AsynchronousApi<QwenTranscriptionParam>();
    createServiceOptions =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .taskGroup(TaskGroup.AUDIO.getValue())
            .task(Task.ASR.getValue())
            .function(Function.TRANSCRIPTION.getValue())
            .isAsyncTask(true)
            .build();
    this.baseUrl = null;
  }

  public QwenTranscriptionResult asyncCall(QwenTranscriptionParam param) {
    try {
      return QwenTranscriptionResult.fromDashScopeResult(
          asyncApi.asyncCall(param, createServiceOptions));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public QwenTranscriptionResult wait(QwenTranscriptionQueryParam queryParam) {
    try {
      return QwenTranscriptionResult.fromDashScopeResult(
          asyncApi.wait(
              queryParam.getTaskId(),
              queryParam.getApiKey(),
              baseUrl,
              queryParam.getCustomHeaders()));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }

  public QwenTranscriptionResult fetch(QwenTranscriptionQueryParam queryParam) {
    try {
      return QwenTranscriptionResult.fromDashScopeResult(
          asyncApi.fetch(
              queryParam.getTaskId(),
              queryParam.getApiKey(),
              baseUrl,
              queryParam.getCustomHeaders()));
    } catch (NoApiKeyException e) {
      throw new ApiException(e);
    }
  }
}
