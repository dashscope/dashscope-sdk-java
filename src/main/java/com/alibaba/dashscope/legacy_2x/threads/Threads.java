package com.alibaba.dashscope.legacy_2x.threads;

import com.alibaba.dashscope.legacy_2x.api.GeneralApi;
import com.alibaba.dashscope.legacy_2x.base.HalfDuplexParamBase;
import com.alibaba.dashscope.legacy_2x.common.DashScopeResult;
import com.alibaba.dashscope.legacy_2x.common.DeletionStatus;
import com.alibaba.dashscope.legacy_2x.common.FlattenResultBase;
import com.alibaba.dashscope.legacy_2x.common.GeneralGetParam;
import com.alibaba.dashscope.legacy_2x.common.UpdateMetadataParam;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.InputRequiredException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import com.alibaba.dashscope.legacy_2x.protocol.ConnectionOptions;
import com.alibaba.dashscope.legacy_2x.protocol.GeneralServiceOption;
import com.alibaba.dashscope.legacy_2x.protocol.HttpMethod;
import com.alibaba.dashscope.legacy_2x.protocol.Protocol;
import com.alibaba.dashscope.legacy_2x.protocol.StreamingMode;
import com.alibaba.dashscope.legacy_2x.utils.StringUtils;

/**
 * @deprecated The threads API is deprecated and will be removed in a future version. Please use the
 *     Responses API instead. See <a
 *     href="https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference">Responses API
 *     documentation</a>
 */
@Deprecated
public final class Threads {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .path("threads")
        .module("assistants")
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
    serviceOption.setPath(StringUtils.format("threads"));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantThread.class);
  }

  public AssistantThread update(String threadId, UpdateMetadataParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("threadId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(StringUtils.format("threads/%s", threadId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantThread.class);
  }

  public AssistantThread retrieve(String threadId) throws ApiException, NoApiKeyException {
    return retrieve(threadId, null);
  }

  public AssistantThread retrieve(String threadId, String apiKey)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(StringUtils.format("threads/%s", threadId));
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
    serviceOption.setPath(StringUtils.format("threads/%s", threadId));
    DashScopeResult result =
        api.delete(GeneralGetParam.builder().apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, DeletionStatus.class);
  }
}
