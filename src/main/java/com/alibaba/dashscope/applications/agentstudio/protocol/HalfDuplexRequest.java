// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol;

import com.alibaba.dashscope.applications.agentstudio.AgentStudioConstants;
import com.alibaba.dashscope.applications.agentstudio.base.HalfDuplexParamBase;
import com.alibaba.dashscope.applications.agentstudio.exception.ApiException;
import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.applications.agentstudio.utils.EncryptionConfig;
import com.alibaba.dashscope.applications.agentstudio.utils.EncryptionUtils;
import com.alibaba.dashscope.applications.agentstudio.utils.JsonUtils;
import com.alibaba.dashscope.applications.agentstudio.utils.StringUtils;
import com.google.gson.JsonObject;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** Builds the HTTP request for a half-duplex AgentStudio call. */
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

  public String getHttpUrl() {
    String baseUrl = AgentStudioConstants.defaultHttpBaseUrl();
    if (serviceOption.getBaseHttpUrl() != null) {
      baseUrl = serviceOption.getBaseHttpUrl();
    }
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    String path = serviceOption.httpUrl();
    // Avoid duplicate path when baseUrl already contains the full path
    if (path != null && !path.isEmpty() && baseUrl.endsWith(path)) {
      return baseUrl;
    }
    return baseUrl + path;
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
    return StringUtils.format(
        "{\"public_key_id\":\"%s\",\"encrypt_key\":\"%s\",\"iv\":\"%s\"}",
        encryptionConfig.getPublicKeyId(),
        EncryptionUtils.RSAEncrypt(base64Cipher, encryptionConfig.getBase64PublicKey()),
        Base64.getEncoder().encodeToString(encryptionConfig.getIv()));
  }

  public HttpRequest getHttpRequest() throws NoApiKeyException, ApiException {
    // Extract and filter custom user agent from param headers
    Map<String, String> paramHeaders = param.getHeaders();
    String customUserAgent = paramHeaders != null ? paramHeaders.get("user-agent") : null;
    Map<String, String> filteredHeaders =
        paramHeaders != null ? new java.util.HashMap<>(paramHeaders) : new java.util.HashMap<>();
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
            customUserAgent,
            serviceOption.getModule());

    if (getHttpMethod() == HttpMethod.GET) {
      return HttpRequest.builder()
          .url(getHttpUrl())
          .httpMethod(getHttpMethod())
          .headers(requestHeaders)
          .parameters(param.getParameters())
          .httpMethod(getHttpMethod())
          .build();
    } else if (getHttpMethod() == HttpMethod.POST
        || getHttpMethod() == HttpMethod.PUT
        || getHttpMethod() == HttpMethod.DELETE) {
      JsonObject body = param.getHttpBody();
      if (isEncryptRequest() && body != null) { // we need to encrypt the input
        this.encryptionConfig =
            EncryptionUtils.generateEncryptionConfig(
                param.getApiKey(), serviceOption.getBaseHttpUrl());
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
          .body(serializeHttpBody(body))
          .httpMethod(getHttpMethod())
          .build();
    } else {
      return HttpRequest.builder().httpMethod(getHttpMethod()).build();
    }
  }

  private String serializeHttpBody(JsonObject body) {
    if (body == null) {
      return null;
    }
    return param.shouldSerializeExplicitNulls() ? body.toString() : JsonUtils.toJson(body);
  }

  public Map<String, String> getHeaders() {
    return param.getHeaders();
  }

  public String getWorkspace() {
    return param.getWorkspace();
  }

  public String getModule() {
    return serviceOption.getModule();
  }
}
