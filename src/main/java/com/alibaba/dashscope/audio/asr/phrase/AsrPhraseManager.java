package com.alibaba.dashscope.audio.asr.phrase;

import com.alibaba.dashscope.api.AsynchronousApi;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;

public final class AsrPhraseManager {
  public static AsrPhraseStatusResult CreatePhrases(AsrPhraseParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.setOperationType(AsrPhraseOperationType.CREATE);
    param.validate();
    AsynchronousApi<AsrPhraseParam> asyncApi = new AsynchronousApi<>();
    AsrPhraseFinetuneOption finetunesServiceOptions =
        AsrPhraseFinetuneOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .isAsyncTask(true)
            .operationType(AsrPhraseOperationType.CREATE)
            .build();
    return AsrPhraseStatusResult.fromDashScopeResult(
        asyncApi.asyncCall(param, finetunesServiceOptions));
  }

  public static AsrPhraseStatusResult UpdatePhrases(AsrPhraseParam param, String phraseId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.setFinetunedOutput(phraseId);
    param.setOperationType(AsrPhraseOperationType.UPDATE);
    param.validate();
    AsynchronousApi<AsrPhraseParam> asyncApi = new AsynchronousApi<>();
    AsrPhraseFinetuneOption finetunesServiceOptions =
        AsrPhraseFinetuneOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.NONE)
            .isAsyncTask(true)
            .operationType(AsrPhraseOperationType.UPDATE)
            .build();
    return AsrPhraseStatusResult.fromDashScopeResult(
        asyncApi.asyncCall(param, finetunesServiceOptions));
  }

  public static AsrPhraseStatusResult QueryPhrase(AsrPhraseParam param, String phraseId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.setFinetunedOutput(phraseId);
    param.setOperationType(AsrPhraseOperationType.QUERY);
    param.validate();
    AsynchronousApi<AsrPhraseParam> asyncApi = new AsynchronousApi<>();
    AsrPhraseFinetuneOption finetunesServiceOptions =
        AsrPhraseFinetuneOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .streamingMode(StreamingMode.NONE)
            .isAsyncTask(false)
            .operationType(AsrPhraseOperationType.QUERY)
            .fineTunedOutput(phraseId)
            .build();
    return AsrPhraseStatusResult.fromDashScopeResult(
        asyncApi.asyncCall(param, finetunesServiceOptions));
  }

  public static AsrPhraseStatusResult DeletePhrase(AsrPhraseParam param, String phraseId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.setFinetunedOutput(phraseId);
    param.setOperationType(AsrPhraseOperationType.DELETE);
    param.validate();
    AsynchronousApi<AsrPhraseParam> asyncApi = new AsynchronousApi<>();
    AsrPhraseFinetuneOption finetunesServiceOptions =
        AsrPhraseFinetuneOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.DELETE)
            .streamingMode(StreamingMode.NONE)
            .isAsyncTask(false)
            .operationType(AsrPhraseOperationType.DELETE)
            .fineTunedOutput(phraseId)
            .build();
    return AsrPhraseStatusResult.fromDashScopeResult(
        asyncApi.asyncCall(param, finetunesServiceOptions));
  }

  public static AsrPhraseStatusResult ListPhrases(AsrPhraseParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.setOperationType(AsrPhraseOperationType.LIST);
    param.validate();
    AsynchronousApi<AsrPhraseParam> asyncApi = new AsynchronousApi<>();
    AsrPhraseFinetuneOption finetunesServiceOptions =
        AsrPhraseFinetuneOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .streamingMode(StreamingMode.NONE)
            .isAsyncTask(false)
            .operationType(AsrPhraseOperationType.LIST)
            .build();
    return AsrPhraseStatusResult.fromDashScopeResult(
        asyncApi.asyncCall(param, finetunesServiceOptions));
  }
}
