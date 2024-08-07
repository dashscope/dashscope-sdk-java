package com.alibaba.dashscope.aigc.completion;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import io.reactivex.Flowable;

/** Support openai compatible api, for test usage. */
public final class ChatCompletions {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .path("chat/completions")
        .build();
  }

  public ChatCompletions() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public ChatCompletions(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  public ChatCompletion call(ChatCompletionParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    DashScopeResult result = api.call(param, serviceOption);
    return ChatCompletion.fromDashScopeResult(result);
  }

  public void call(ChatCompletionParam param, ResultCallback<ChatCompletion> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    api.call(
        param,
        serviceOption,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(ChatCompletion.fromDashScopeResult(message));
          }

          @Override
          public void onComplete() {
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            callback.onError(e);
          }
        });
  }

  public Flowable<ChatCompletionChunk> streamCall(ChatCompletionParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    return api.streamCall(param, serviceOption)
        .map(item -> ChatCompletionChunk.fromDashScopeResult(item));
  }

  public void streamCall(ChatCompletionParam param, ResultCallback<ChatCompletionChunk> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    api.streamCall(
        param,
        serviceOption,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            callback.onEvent(ChatCompletionChunk.fromDashScopeResult(msg));
          }

          @Override
          public void onComplete() {
            callback.onComplete();
          }

          @Override
          public void onError(Exception e) {
            callback.onError(e);
          }
        });
  }
}
