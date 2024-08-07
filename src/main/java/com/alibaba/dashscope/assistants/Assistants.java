package com.alibaba.dashscope.assistants;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.DeletionStatus;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class Assistants {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  private GeneralServiceOption defaultServiceOption() {
    return GeneralServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.OUT)
        .path("assistants")
        .build();
  }

  public Assistants() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public Assistants(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  public Assistant create(AssistantParam param) throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("assistants"));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Assistant.class);
  }

  public Assistant update(String assistantId, AssistantParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.equals("")) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("assistants/%s", assistantId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Assistant.class);
  }

  public ListResult<Assistant> list(GeneralListParam listParam)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath("assistants");
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<Assistant>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  public Assistant retrieve(String assistantId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieve(assistantId, null);
  }

  public Assistant retrieve(String assistantId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieve(assistantId, apiKey, new HashMap<>());
  }

  public Assistant retrieve(String assistantId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("assistants/%s", assistantId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Assistant.class);
  }

  public DeletionStatus delete(String assistantId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return delete(assistantId, null);
  }

  public DeletionStatus delete(String assistantId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return delete(assistantId, apiKey, new HashMap<>());
  }

  public DeletionStatus delete(String assistantId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.DELETE);
    serviceOption.setPath(String.format("assistants/%s", assistantId));
    DashScopeResult result =
        api.delete(
            GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, DeletionStatus.class);
  }

  public AssistantFile createFile(String assistantId, AssistantFileParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("assistants/%s/files", assistantId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantFile.class);
  }

  public ListResult<AssistantFile> listFiles(String assistantId, GeneralListParam listParam)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("assistants/%s/files", assistantId));
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<AssistantFile>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  public AssistantFile retrieveFile(String assistantId, String fileId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieveFile(assistantId, fileId, null);
  }

  public AssistantFile retrieveFile(String assistantId, String fileId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieveFile(assistantId, fileId, apiKey, new HashMap<>());
  }

  public AssistantFile retrieveFile(
      String assistantId, String fileId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty() || fileId == null || fileId.isEmpty()) {
      throw new InputRequiredException("assistantId and fileId are required!");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("assistants/%s/files/%s", assistantId, fileId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantFile.class);
  }
}
