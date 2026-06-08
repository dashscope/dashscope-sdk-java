// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.app;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.StreamingMode;
import io.reactivex.Flowable;

/**
 * Title Ap completion calls.<br>
 * Description App completion calls.<br>
 * Created at 2024-02-23 15:38
 *
 * @since jdk8
 */
public class Application {
  private final SynchronizeHalfDuplexApi<ApplicationParam> syncApi;

  private final ApiServiceOption serviceOption;

  private ApiServiceOption defaultServiceOption() {
    return ApiServiceOption.builder()
        .httpMethod(HttpMethod.POST)
        .outputMode(OutputMode.ACCUMULATE)
        .build();
  }

  public Application() {
    serviceOption = defaultServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public Application(String baseUrl) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  /**
   * app completion call for http request
   *
   * @param param app completion params
   * @return app completion result
   * @throws ApiException failed to request api
   * @throws NoApiKeyException can not find api key
   * @throws InputRequiredException missing required inputs
   */
  public ApplicationResult call(ApplicationParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    setRequestOption(serviceOption, param.getAppId());
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);

    return ApplicationResult.fromDashScopeResult(syncApi.call(param));
  }

  /**
   * app completion call for http request by sse stream
   *
   * @param param app completion params
   * @return flowable stream of app completion result
   * @throws ApiException failed to request api
   * @throws NoApiKeyException can not find api key
   * @throws InputRequiredException missing required inputs
   */
  public Flowable<ApplicationResult> streamCall(ApplicationParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    setRequestOption(serviceOption, param.getAppId());
    serviceOption.setIsSSE(true);
    serviceOption.setStreamingMode(StreamingMode.OUT);

    return syncApi.streamCall(param).map(ApplicationResult::fromDashScopeResult);
  }

  private void setRequestOption(ApiServiceOption serviceOption, String resourceId) {
    if (serviceOption == null) {
      return;
    }

    serviceOption.setIsService(false);
    serviceOption.setTaskGroup("apps");
    serviceOption.setTask(resourceId);
    serviceOption.setFunction("completion");
    //        serviceOption.setResource("apps");
    //        serviceOption.setResourceId(resourceId);
    //        serviceOption.setAction("completion");
  }
}
