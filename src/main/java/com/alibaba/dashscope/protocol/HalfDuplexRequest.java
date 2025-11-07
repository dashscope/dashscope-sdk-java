// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.EncryptionConfig;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HalfDuplexRequest {
  HalfDuplexParamBase param;
  ServiceOption serviceOption;
  EncryptionConfig encryptionConfig;

  public HalfDuplexRequest(HalfDuplexParamBase param, ServiceOption option) {
    this.param = param;
    this.serviceOption = option;
    encryptionConfig = null;
  }

  public boolean getIsFlatten() {
    return serviceOption.getIsFlatten();
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

  public String getBaseWebSocketUrl() {
    return serviceOption.getBaseWebSocketUrl();
  }

  public String getHttpUrl() {
    String baseUrl = Constants.baseHttpApiUrl;
    if (serviceOption.getBaseHttpUrl() != null) {
      baseUrl = serviceOption.getBaseHttpUrl();
    }
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

  public Boolean isEncryptRequest() {
    return param.getEnableEncrypt();
  }

  public EncryptionConfig getEncryptionConfig() {
    return encryptionConfig;
  }

  private String getEncryptionKeyHeader(EncryptionConfig encryptionConfig) throws ApiException {
    byte[] cipherBytes = encryptionConfig.getAESEncryptKey().getEncoded();
    String base64Cipher = Base64.getEncoder().encodeToString(cipherBytes);
    return String.format(
        "{\"public_key_id\":\"%s\",\"encrypt_key\":\"%s\",\"iv\":\"%s\"}",
        encryptionConfig.getPublicKeyId(),
        EncryptionUtils.RSAEncrypt(base64Cipher, encryptionConfig.getBase64PublicKey()),
        Base64.getEncoder().encodeToString(encryptionConfig.getIv()));
  }

  public HttpRequest getHttpRequest() throws NoApiKeyException, ApiException {
    // Extract and filter custom user agent from param headers
    Map<String, String> paramHeaders = param.getHeaders();
    String customUserAgent = paramHeaders != null ? paramHeaders.get("user-agent") : null;
    Map<String, String> filteredHeaders = paramHeaders != null ?
            new java.util.HashMap<>(paramHeaders) : new java.util.HashMap<>();
    filteredHeaders.remove("user-agent");

    Map<String, String> requestHeaders =
        DashScopeHeaders.buildHttpHeaders(
            param.getApiKey(),
            param.isSecurityCheck(),
            Protocol.HTTP,
            serviceOption.getIsSSE(),
            serviceOption.getIsAsyncTask(),
            param.getWorkspace(),
            filteredHeaders,
            customUserAgent);

    if (getHttpMethod() == HttpMethod.GET) {
      return HttpRequest.builder()
          .url(getHttpUrl())
          .httpMethod(getHttpMethod())
          .headers(requestHeaders)
          .parameters(param.getParameters())
          .httpMethod(getHttpMethod())
          .build();
    } else if (getHttpMethod() == HttpMethod.POST || getHttpMethod() == HttpMethod.DELETE) {
      JsonObject body = param.getHttpBody();
      if (isEncryptRequest() && body != null) { // we need to encrypt the input
        this.encryptionConfig = EncryptionUtils.generateEncryptionConfig(param.getApiKey());
        requestHeaders.put("X-DashScope-EncryptionKey", getEncryptionKeyHeader(encryptionConfig));
        JsonObject input = body.get("input").getAsJsonObject();
        String chiperInput =
            EncryptionUtils.AESEncrypt(
                JsonUtils.toJson(input),
                encryptionConfig.getAESEncryptKey(),
                encryptionConfig.getIv());
        body.addProperty("input", chiperInput);
      }
      return HttpRequest.builder()
          .url(getHttpUrl())
          .headers(requestHeaders)
          .body(body == null ? null : JsonUtils.toJson(body))
          .httpMethod(getHttpMethod())
          .build();
    } else {
      return HttpRequest.builder().httpMethod(getHttpMethod()).build();
    }
  }

  public JsonObject getWebSocketPayload() {
    JsonObject request = new JsonObject();
    request.addProperty(ApiKeywords.MODEL, param.getModel());
    request.addProperty(ApiKeywords.TASK_GROUP, serviceOption.getTaskGroup());
    request.addProperty(ApiKeywords.TASK, serviceOption.getTask());
    request.addProperty(ApiKeywords.FUNCTION, serviceOption.getFunction());
    if (param.getBinaryData() == null) {
      request.add(ApiKeywords.INPUT, (JsonObject) param.getInput());
    }
    if (param.getParameters() != null) {
      request.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    if (param.getResources() != null) {
      request.add(ApiKeywords.RESOURCES, (JsonElement) param.getResources());
    }
    return request;
  }

  public JsonObject getWebSocketPayload(Object data) {
    JsonObject request = new JsonObject();
    request.addProperty(ApiKeywords.MODEL, param.getModel());
    request.addProperty(ApiKeywords.TASK_GROUP, serviceOption.getTaskGroup());
    request.addProperty(ApiKeywords.TASK, serviceOption.getTask());
    request.addProperty(ApiKeywords.FUNCTION, serviceOption.getFunction());
    if (data instanceof String) {
      request.add(ApiKeywords.INPUT, (JsonObject) param.getInput());
    } else {
      request.add(ApiKeywords.INPUT, new JsonObject()); // empty input
    }
    if (param.getParameters() != null) {
      request.add(ApiKeywords.PARAMETERS, JsonUtils.parametersToJsonObject(param.getParameters()));
    }
    if (param.getResources() != null) {
      request.add(ApiKeywords.RESOURCES, (JsonObject) param.getResources());
    }
    return request;
  }

  public JsonObject getStartTaskMessage() {
    JsonObject header = new JsonObject();
    header.addProperty(ApiKeywords.ACTION, WebSocketEventType.RUN_TASK.getValue());
    if (param.getParameters() != null && param.getParameters().containsKey("pre_task_id")) {
      header.addProperty(ApiKeywords.TASKID, (String) param.getParameters().get("pre_task_id"));
    } else {
      header.addProperty(ApiKeywords.TASKID, UUID.randomUUID().toString());
    }
    header.addProperty(ApiKeywords.STREAMING, serviceOption.getStreamingMode().getValue());
    JsonObject wsMessage = new JsonObject();
    wsMessage.add(ApiKeywords.HEADER, header);
    wsMessage.add(ApiKeywords.PAYLOAD, getWebSocketPayload());
    return wsMessage;
  }

  public ByteBuffer getWebsocketBinaryData() {
    return param.getBinaryData();
  }

  public Map<String, String> getHeaders() {
    return param.getHeaders();
  }

  public String getWorkspace() {
    return param.getWorkspace();
  }
}
