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
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @deprecated The Assistants API (com.alibaba.dashscope.assistants) is deprecated and will be
 *     removed in a future release. Please migrate to the Responses API. See
 *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration details.
 */
@Deprecated
public final class Assistants {
  private final GeneralApi<HalfDuplexParamBase> api;
  private final GeneralServiceOption serviceOption;

  // Connection pre-warming mechanism
  private static final AtomicBoolean connectionPreWarmed = new AtomicBoolean(false);

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
    // Pre-warm connection on first instance creation
    preWarmConnection();
  }

  public Assistants(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
    // Pre-warm connection on first instance creation
    preWarmConnection();
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public Assistant create(AssistantParam param) throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(StringUtils.format("assistants"));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Assistant.class);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public Assistant update(String assistantId, AssistantParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.equals("")) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(StringUtils.format("assistants/%s", assistantId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Assistant.class);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public ListResult<Assistant> list(GeneralListParam listParam)
      throws ApiException, NoApiKeyException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath("assistants");
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<Assistant>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public Assistant retrieve(String assistantId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieve(assistantId, null);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public Assistant retrieve(String assistantId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieve(assistantId, apiKey, new HashMap<>());
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public Assistant retrieve(String assistantId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(StringUtils.format("assistants/%s", assistantId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, Assistant.class);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public DeletionStatus delete(String assistantId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return delete(assistantId, null);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public DeletionStatus delete(String assistantId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return delete(assistantId, apiKey, new HashMap<>());
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public DeletionStatus delete(String assistantId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.DELETE);
    serviceOption.setPath(StringUtils.format("assistants/%s", assistantId));
    DashScopeResult result =
        api.delete(
            GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, DeletionStatus.class);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public AssistantFile createFile(String assistantId, AssistantFileParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(StringUtils.format("assistants/%s/files", assistantId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantFile.class);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public ListResult<AssistantFile> listFiles(String assistantId, GeneralListParam listParam)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty()) {
      throw new InputRequiredException("assistantId is required!");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(StringUtils.format("assistants/%s/files", assistantId));
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<AssistantFile>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public AssistantFile retrieveFile(String assistantId, String fileId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieveFile(assistantId, fileId, null);
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public AssistantFile retrieveFile(String assistantId, String fileId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieveFile(assistantId, fileId, apiKey, new HashMap<>());
  }

  /**
   * @deprecated The Assistants API is deprecated and will be removed in a future release. Please
   *     migrate to the Responses API. See
   *     https://help.aliyun.com/zh/model-studio/synchronous-call-api-reference for migration
   *     details.
   */
  @Deprecated
  public AssistantFile retrieveFile(
      String assistantId, String fileId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (assistantId == null || assistantId.isEmpty() || fileId == null || fileId.isEmpty()) {
      throw new InputRequiredException("assistantId and fileId are required!");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(StringUtils.format("assistants/%s/files/%s", assistantId, fileId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, AssistantFile.class);
  }

  /**
   * Pre-warm the HTTP connection to reduce latency for first API call. Uses a lightweight list
   * request to establish connection pool.
   */
  private void preWarmConnection() {
    if (connectionPreWarmed.compareAndSet(false, true)) {
      try {
        // Lightweight GET request to establish connection
        GeneralServiceOption warmupOption =
            GeneralServiceOption.builder()
                .protocol(Protocol.HTTP)
                .httpMethod(HttpMethod.GET)
                .streamingMode(StreamingMode.OUT)
                .path("assistants")
                .build();

        if (serviceOption.getBaseHttpUrl() != null) {
          warmupOption.setBaseHttpUrl(serviceOption.getBaseHttpUrl());
        }

        api.get(GeneralListParam.builder().limit(1L).build(), warmupOption);
      } catch (Exception e) {
        // Reset flag to allow retry if pre-warming failed
        connectionPreWarmed.set(false);
      }
    }
  }
}
