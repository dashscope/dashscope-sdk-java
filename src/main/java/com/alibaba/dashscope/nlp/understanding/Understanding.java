package com.alibaba.dashscope.nlp.understanding;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Understanding {
  private final SynchronizeHalfDuplexApi<UnderstandingParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String OPENNLU_V1 = "opennlu-v1";
  }

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.NLP.getValue())
        .task(Task.NLU.getValue())
        .function(Function.UNDERSTANDING.getValue())
        .isSSE(false)
        .streamingMode(StreamingMode.NONE)
        .build();
  }

  public Understanding() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Understanding(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Understanding(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  /**
   * Call the server to get the whole result, only http protocol
   *
   * @param param The input param of class `UnderstandingParam`.
   * @return The output structure of `UnderstandingResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws InputRequiredException Missing inputs.
   */
  public UnderstandingResult call(UnderstandingParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    return UnderstandingResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `UnderstandingParam`.
   * @param callback The callback to receive response, the template class is `UnderstandingResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public void call(UnderstandingParam param, ResultCallback<UnderstandingResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(UnderstandingResult.fromDashScopeResult(message));
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
