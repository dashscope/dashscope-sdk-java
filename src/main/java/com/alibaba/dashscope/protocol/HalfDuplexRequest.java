// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.UUID;

public class HalfDuplexRequest {
  HalfDuplexParamBase param;
  ServiceOption serviceOption;

  public HalfDuplexRequest(HalfDuplexParamBase param, ServiceOption option) {
    this.param = param;
    this.serviceOption = option;
  }

  public String getApiKey() {
    return param.getApiKey();
  }

  public StreamingMode getStreamingMode() {
    return serviceOption.getStreamingMode();
  }

  public OutputMode getOutputMode() {
    return serviceOption.getOutputMode();
  }

  public String getHttpUrl() {
    String baseUrl = Constants.baseHttpApiUrl;
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl + serviceOption.httpUrl();
  }

  public boolean isSecurityCheck() {
    return param.isSecurityCheck();
  }

  public HttpMethod getHttpMethod() {
    return serviceOption.getHttpMethod();
  }

  public HttpRequest getHttpRequest() throws NoApiKeyException, ApiException {
    if (getHttpMethod() == HttpMethod.GET) {
      return HttpRequest.builder()
          .url(getHttpUrl())
          .httpMethod(getHttpMethod())
          .headers(
              DashScopeHeaders.buildHttpHeaders(
                  param.getApiKey(),
                  param.isSecurityCheck(),
                  Protocol.HTTP,
                  serviceOption.getIsSSE(),
                  serviceOption.getIsAsyncTask()))
          .httpMethod(getHttpMethod())
          .build();
    } else if (getHttpMethod() == HttpMethod.POST) {
      JsonObject body = param.getHttpBody();
      return HttpRequest.builder()
          .url(getHttpUrl())
          .headers(
              DashScopeHeaders.buildHttpHeaders(
                  param.getApiKey(),
                  param.isSecurityCheck(),
                  Protocol.HTTP,
                  serviceOption.getIsSSE(),
                  serviceOption.getIsAsyncTask()))
          .body(body == null ? null : JsonUtils.toJson(body))
          .httpMethod(getHttpMethod())
          .build();
    } else {
      return HttpRequest.builder().httpMethod(getHttpMethod()).build();
    }
  }

  public JsonObject getWebSocketPayload() {
    JsonObject request = new JsonObject();
    request.addProperty("model", param.getModel());
    request.addProperty("task_group", serviceOption.getTaskGroup());
    request.addProperty("task", serviceOption.getTask());
    request.addProperty("function", serviceOption.getFunction());
    if (param.getBinaryData() == null) {
      request.add("input", (JsonObject) param.getInput());
    }
    if (param.getParameters() != null) {
      request.add("parameters", JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    return request;
  }

  public JsonObject getWebSocketPayload(Object data) {
    JsonObject request = new JsonObject();
    request.addProperty("model", param.getModel());
    request.addProperty("task_group", serviceOption.getTaskGroup());
    request.addProperty("task", serviceOption.getTask());
    request.addProperty("function", serviceOption.getFunction());
    if (data instanceof String) {
      request.add("input", (JsonObject) param.getInput());
    } else {
      request.add("input", new JsonObject()); // empty input
    }
    if (param.getParameters() != null) {
      request.add("parameters", JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    return request;
  }

  public JsonObject getStartTaskMessage() {
    JsonObject header = new JsonObject();
    header.addProperty("action", WebSocketEventType.RUN_TASK.getValue());
    header.addProperty("task_id", UUID.randomUUID().toString());
    header.addProperty("streaming", serviceOption.getStreamingMode().getValue());
    JsonObject wsMessage = new JsonObject();
    wsMessage.add("header", header);
    wsMessage.add("payload", getWebSocketPayload());
    return wsMessage;
  }

  public ByteBuffer getWebsocketBinaryData() {
    return param.getBinaryData();
  }
}
