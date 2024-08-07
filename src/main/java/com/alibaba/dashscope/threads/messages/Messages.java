package com.alibaba.dashscope.threads.messages;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.FlattenResultBase;
import com.alibaba.dashscope.common.GeneralGetParam;
import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.common.UpdateMetadataParam;
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

public final class Messages {
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

  public Messages() {
    serviceOption = defaultServiceOption();
    api = new GeneralApi<>();
  }

  public Messages(String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultServiceOption();
    serviceOption.setBaseHttpUrl(baseUrl);
    api = new GeneralApi<>(connectionOptions);
  }

  public ThreadMessage create(String threadId, MessageParamBase param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/messages", threadId));
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("The threadId is required.");
    }
    param.validate();
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, ThreadMessage.class);
  }

  public ThreadMessage update(String threadId, String messageId, UpdateMetadataParam param)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.isEmpty() || messageId == null || messageId.isEmpty()) {
      throw new InputRequiredException("threadId  and messageId  are required!");
    }
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("threads/%s/messages/%s", threadId, messageId));
    DashScopeResult result = api.call(param, serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, ThreadMessage.class);
  }

  public ListResult<ThreadMessage> list(String threadId, GeneralListParam listParam)
      throws ApiException, NoApiKeyException, InputRequiredException {
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/messages", threadId));
    if (threadId == null || threadId.equals("")) {
      throw new InputRequiredException("The threadId is required.");
    }
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<ThreadMessage>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }

  public ThreadMessage retrieve(String threadId, String messageId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieve(threadId, messageId, null);
  }

  public ThreadMessage retrieve(String threadId, String messageId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieve(threadId, messageId, apiKey, new HashMap<>());
  }

  public ThreadMessage retrieve(
      String threadId, String messageId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.isEmpty() || messageId == null || messageId.isEmpty()) {
      throw new InputRequiredException("The threadId and messageId are required.");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/messages/%s", threadId, messageId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, ThreadMessage.class);
  }

  public MessageFile retrieveFile(String threadId, String messageId, String fileId)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieveFile(threadId, messageId, fileId, null);
  }

  public MessageFile retrieveFile(String threadId, String messageId, String fileId, String apiKey)
      throws ApiException, NoApiKeyException, InputRequiredException {
    return retrieveFile(threadId, messageId, fileId, apiKey, new HashMap<>());
  }

  public MessageFile retrieveFile(
      String threadId, String messageId, String fileId, String apiKey, Map<String, String> headers)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null
        || threadId.isEmpty()
        || messageId == null
        || messageId.isEmpty()
        || fileId == null
        || fileId.isEmpty()) {
      throw new InputRequiredException("The threadId, messageId and fileId are required.");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(
        String.format("threads/%s/messages/%s/files/%s", threadId, messageId, fileId));
    DashScopeResult result =
        api.get(GeneralGetParam.builder().headers(headers).apiKey(apiKey).build(), serviceOption);
    return FlattenResultBase.fromDashScopeResult(result, MessageFile.class);
  }

  public ListResult<MessageFile> listFiles(
      String threadId, String messageId, GeneralListParam listParam)
      throws ApiException, NoApiKeyException, InputRequiredException {
    if (threadId == null || threadId.isEmpty() || messageId == null || messageId.isEmpty()) {
      throw new InputRequiredException("The threadId and messageId are required.");
    }
    serviceOption.setHttpMethod(HttpMethod.GET);
    serviceOption.setPath(String.format("threads/%s/messages/%s/files", threadId, messageId));
    DashScopeResult result = api.get(listParam, serviceOption);
    Type typeOfT = new TypeToken<ListResult<MessageFile>>() {}.getType();
    return FlattenResultBase.fromDashScopeResult(result, typeOfT);
  }
}
