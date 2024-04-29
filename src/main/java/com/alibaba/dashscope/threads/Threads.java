package com.alibaba.dashscope.threads;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.DeletionStatus;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.common.UpdateMetadataParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;

public final class Threads {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .path("threads")
        .build();
  }

  public Threads() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public Threads(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  public AssistantThread create(ThreadParam param) throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads"));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantThread.class);
  }

  public AssistantThread update(String threadId, UpdateMetadataParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("threadId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s", threadId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantThread.class);
  }

  public AssistantThread retrieve(String threadId) throws ApiException, NoApiKeyException {
    return retrieve(threadId, null);
  }

  public AssistantThread retrieve(String threadId, String apiKey)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s", threadId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantThread.class);
  }

  public DeletionStatus delete(String threadId) throws ApiException, NoApiKeyException {
    return delete(threadId, null);
  }

  public DeletionStatus delete(String threadId, String apiKey)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.DELETE);
    serviceOption.setPath(String.format("threads/%s", threadId));
    DashScopeResult result =
        api.delete(GeneralGetParam.builder().apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, DeletionStatus.class);
  }
}
