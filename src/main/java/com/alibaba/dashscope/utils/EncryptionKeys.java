package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;

public class EncryptionKeys {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.GET)
        .streamingMode(StreamingMode.NONE)
        .path("public-keys/latest")
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
