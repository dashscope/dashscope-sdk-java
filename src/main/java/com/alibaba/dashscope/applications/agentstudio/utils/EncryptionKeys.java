// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import com.alibaba.dashscope.applications.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.applications.agentstudio.api.GeneralApi;
import com.alibaba.dashscope.applications.agentstudio.base.HalfDuplexParamBase;
import com.alibaba.dashscope.applications.agentstudio.common.DashScopeResult;
import com.alibaba.dashscope.applications.agentstudio.common.GeneralGetParam;
import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.applications.agentstudio.protocol.ConnectionOptions;
import com.alibaba.dashscope.applications.agentstudio.protocol.GeneralServiceOption;
import com.alibaba.dashscope.applications.agentstudio.protocol.HttpMethod;
import com.alibaba.dashscope.applications.agentstudio.protocol.Protocol;
import com.alibaba.dashscope.applications.agentstudio.protocol.StreamingMode;

/** Fetches the latest public key used for encrypted AgentStudio requests. */
public class EncryptionKeys {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.GET)
        .streamingMode(StreamingMode.NONE)
        .path("public-keys/latest")
        .module(AgentStudioConstants.SDK_MODULE)
        .build();
  }

  public EncryptionKeys() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public EncryptionKeys(String baseUrl) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>();
  }

  public EncryptionKeys(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  public EncryptionKey get() throws ApiException, NoApiKeyException {
    return get(null);
  }

  public EncryptionKey get(String apiKey) throws ApiException, NoApiKeyException {
    DashScopeResult result =
        api.get(GeneralGetParam.builder().apiKey(apiKey).build(), serviceOption);
    return EncryptionKey.fromDashScopeResult(result);
  }
}
