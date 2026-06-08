package com.alibaba.dashscope.aigc.codegeneration;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CodeGeneration {
  private final SynchronizeHalfDuplexApi<CodeGenerationParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String TONGYI_LINGMA_V1 = "tongyi-lingma-v1";
  }

  public static class Scenes {
    public static final String CUSTOM = "custom";
    public static final String NL2CODE = "nl2code";
    public static final String CODE2COMMENT = "code2comment";
    public static final String CODE2EXPLAIN = "code2explain";
    public static final String COMMIT2MSG = "commit2msg";
    public static final String UNIT_TEST = "unittest";
    public static final String CODE_QA = "codeqa";
    public static final String NL2SQL = "nl2sql";
  }

  private ApiServiceOption defaultServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.AIGC.getValue())
        .task(Task.CODE_GENERATION.getValue())
        .function(Function.GENERATION.getValue())
        .build();
  }

  public CodeGeneration() {
    serviceOption = defaultServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public CodeGeneration(String protocol) {
    serviceOption = defaultServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public CodeGeneration(String protocol, String baseUrl) {
    serviceOption = defaultServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public CodeGeneration(String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `CodeGenerationParam`.
   * @return The output structure of `CodeGenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws InputRequiredException Missing inputs.
   */
  public CodeGenerationResult call(CodeGenerationParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    return CodeGenerationResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `CodeGenerationParam`.
   * @param callback The callback to receive response, the template class is `CodeGenerationResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public void call(CodeGenerationParam param, ResultCallback<CodeGenerationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    syncApi.call(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {
            callback.onEvent(CodeGenerationResult.fromDashScopeResult(message));
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

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `CodeGenerationParam`.
   * @return A `Flowable` of the output structure.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException Missing inputs.
   */
  public Flowable<CodeGenerationResult> streamCall(CodeGenerationParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    return syncApi.streamCall(param).map(item -> CodeGenerationResult.fromDashScopeResult(item));
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `CodeGenerationParam`.
   * @param callback The result callback.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   * @throws InputRequiredException The input field is missing.
   */
  public void streamCall(CodeGenerationParam param, ResultCallback<CodeGenerationResult> callback)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult msg) {
            callback.onEvent(CodeGenerationResult.fromDashScopeResult(msg));
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
