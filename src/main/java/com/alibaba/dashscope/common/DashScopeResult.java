// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class DashScopeResult extends Result {
  private Object output;
  private String event;

  public Boolean isBinaryOutput() {
    return output instanceof ByteBuffer;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <T extends Result> T fromResponse(Protocol protocol, NetworkResponse response)
      throws ApiException {
    this.setHeaders(changeHeaders(response.getHeaders()));
    if (protocol == Protocol.WEBSOCKET) {
      if (response.getBinary() == null) {
        String message = response.getMessage();
        if (message == null || message.isEmpty()) {
          log.warn(
              "WebSocket response message is null or empty, httpStatusCode: {}",
              response.getHttpStatusCode());
          setInternalError();
          return (T) this;
        }
        fromWebSocketMessage(message);
      } else {
        this.output = response.getBinary();
      }
    } else {
      String message = response.getMessage();
      if (message == null || message.isEmpty()) {
        log.warn(
            "HTTP response message is null or empty, httpStatusCode: {}",
            response.getHttpStatusCode());
        setInternalError();
        return (T) this;
      }
      JsonObject jsonObject = parseJson(message, "Failed to parse HTTP response as JSON: {}");
      if (jsonObject == null) {
        return (T) this;
      }
      if (response.getHttpStatusCode() != null) {
        this.setStatusCode(response.getHttpStatusCode());
      }
      handleOutputField(jsonObject);
      populateFromHttpJson(jsonObject);
      handleDataField(jsonObject);
    }
    return (T) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult) throws ApiException {
    if (!isFlattenResult) {
      return fromResponse(protocol, response);
    }
    this.setHeaders(changeHeaders(response.getHeaders()));
    if (protocol == Protocol.WEBSOCKET) {
      if (response.getBinary() == null) {
        this.output = parseJson(response.getMessage(), "Failed to parse WebSocket message");
      } else {
        this.output = response.getBinary();
      }
    } else {
      this.output = parseJson(response.getMessage(), "Failed to parse HTTP response message");
      this.event = response.getEvent();
    }
    return (T) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult, HalfDuplexRequest req)
      throws ApiException {
    this.setHeaders(changeHeaders(response.getHeaders()));
    if ((response.getHeaders().containsKey("X-DashScope-OutputEncrypted".toLowerCase())
            || req.isEncryptRequest())
        && protocol == Protocol.HTTP) {
      if (response.getHttpStatusCode() != null) {
        this.setStatusCode(response.getHttpStatusCode());
      }
      String encryptedMessage = response.getMessage();
      if (encryptedMessage == null || encryptedMessage.isEmpty()) {
        log.warn(
            "Encrypted HTTP response message is null or empty, httpStatusCode: {}",
            response.getHttpStatusCode());
        setInternalError();
        return (T) this;
      }
      JsonObject jsonObject =
          parseJson(encryptedMessage, "Failed to parse encrypted HTTP response message");
      if (jsonObject == null) {
        return (T) this;
      }
      String encryptedOutput =
          jsonObject.get(ApiKeywords.OUTPUT).isJsonNull()
              ? null
              : jsonObject.get(ApiKeywords.OUTPUT).getAsString();
      if (encryptedOutput != null) {
        String plainOutput =
            EncryptionUtils.AESDecrypt(
                encryptedOutput,
                req.getEncryptionConfig().getAESEncryptKey(),
                req.getEncryptionConfig().getIv());
        this.output = parseJson(plainOutput, "Failed to parse decrypted output");
      } else {
        this.output = null;
      }
      populateFromHttpJson(jsonObject);
      return (T) this;
    }
    return fromResponse(protocol, response, isFlattenResult);
  }

  private void fromWebSocketMessage(String message) {
    JsonObject jsonObject = parseJson(message, "Failed to parse WebSocket message");
    if (jsonObject == null) {
      return;
    }
    if (jsonObject.has(ApiKeywords.HEADER)) {
      JsonObject headers = jsonObject.get(ApiKeywords.HEADER).getAsJsonObject();
      if (headers.has(ApiKeywords.TASKID)) {
        this.setRequestId(headers.get(ApiKeywords.TASKID).getAsString());
      }
      this.setStatusCode(
          headers.has(ApiKeywords.STATUS_CODE) && !headers.get(ApiKeywords.STATUS_CODE).isJsonNull()
              ? headers.get(ApiKeywords.STATUS_CODE).getAsInt()
              : 200);
      this.setCode(
          headers.has(ApiKeywords.ERROR_CODE) && !headers.get(ApiKeywords.ERROR_CODE).isJsonNull()
              ? headers.get(ApiKeywords.ERROR_CODE).getAsString()
              : "");
      this.setMessage(
          headers.has(ApiKeywords.ERROR_MESSAGE)
                  && !headers.get(ApiKeywords.ERROR_MESSAGE).isJsonNull()
              ? headers.get(ApiKeywords.ERROR_MESSAGE).getAsString()
              : "");
    }
    if (jsonObject.has(ApiKeywords.PAYLOAD)) {
      JsonObject payload = jsonObject.getAsJsonObject(ApiKeywords.PAYLOAD);
      if (payload.has(ApiKeywords.OUTPUT)) {
        this.output =
            payload.get(ApiKeywords.OUTPUT).isJsonNull() ? null : payload.get(ApiKeywords.OUTPUT);
      }
      if (payload.has(ApiKeywords.USAGE)) {
        this.setUsage(
            payload.get(ApiKeywords.USAGE).isJsonNull() ? null : payload.get(ApiKeywords.USAGE));
      }
    }
  }

  private void handleOutputField(JsonObject jsonObject) {
    // Handle OUTPUT field first
    if (this.output == null && jsonObject.has(ApiKeywords.OUTPUT)) {
      JsonElement outputElement = jsonObject.get(ApiKeywords.OUTPUT);
      this.output = (outputElement == null || outputElement.isJsonNull()) ? null : outputElement;
    }
  }

  private void handleDataField(JsonObject jsonObject) {
    // Handle DATA field only if OUTPUT was not present
    if (this.output == null && jsonObject.has(ApiKeywords.DATA)) {
      jsonObject.remove(ApiKeywords.REQUEST_ID);
      this.output = jsonObject;
    }
  }

  private void populateFromHttpJson(JsonObject jsonObject) {
    if (jsonObject.has(ApiKeywords.USAGE)) {
      this.setUsage(
          jsonObject.get(ApiKeywords.USAGE).isJsonNull()
              ? null
              : jsonObject.get(ApiKeywords.USAGE).getAsJsonObject());
    }
    if (jsonObject.has(ApiKeywords.REQUEST_ID)) {
      this.setRequestId(jsonObject.get(ApiKeywords.REQUEST_ID).getAsString());
    }
    if (jsonObject.has(ApiKeywords.STATUS_CODE)) {
      this.setStatusCode(
          jsonObject.get(ApiKeywords.STATUS_CODE).isJsonNull()
              ? null
              : jsonObject.get(ApiKeywords.STATUS_CODE).getAsInt());
    }
    this.setCode(
        jsonObject.has(ApiKeywords.CODE) && !jsonObject.get(ApiKeywords.CODE).isJsonNull()
            ? jsonObject.get(ApiKeywords.CODE).getAsString()
            : "");
    this.setMessage(
        jsonObject.has(ApiKeywords.MESSAGE) && !jsonObject.get(ApiKeywords.MESSAGE).isJsonNull()
            ? jsonObject.get(ApiKeywords.MESSAGE).getAsString()
            : "");
  }

  private JsonObject parseJson(String raw, String logMsg) {
    try {
      return JsonUtils.parse(raw);
    } catch (Exception e) {
      log.error(logMsg, e);
      setInternalError();
      return null;
    }
  }

  private void setInternalError() {
    this.output = null;
    this.setStatusCode(PublicErrorDef.INTERNAL_ERROR.getStatusCode());
    this.setCode(PublicErrorDef.INTERNAL_ERROR.getErrorCode());
    this.setMessage(PublicErrorDef.INTERNAL_ERROR.getErrorMsg());
  }

  private Map<String, Object> changeHeaders(Map<String, List<String>> headers) {
    if (headers == null || headers.isEmpty()) {
      return null;
    }
    return headers.entrySet().stream()
        .filter(entry -> entry.getKey() != null)
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                  List<String> values = entry.getValue();
                  return (values == null || values.isEmpty()) ? "" : String.join(",", values);
                },
                (v1, v2) -> v1,
                java.util.LinkedHashMap::new));
  }

  /** Keyword-to-status mapping for legacy / non-standard error codes. */
  private static final Map<String, Integer> LEGACY_ERROR_KEYWORDS = new LinkedHashMap<>();

  static {
    LEGACY_ERROR_KEYWORDS.put("InvalidParameter", 400);
    LEGACY_ERROR_KEYWORDS.put("BadRequest", 400);
    LEGACY_ERROR_KEYWORDS.put("DataInspection", 400);
    LEGACY_ERROR_KEYWORDS.put("Inspection", 400);
    LEGACY_ERROR_KEYWORDS.put("Unauthorized", 401);
    LEGACY_ERROR_KEYWORDS.put("ApiKey", 401);
    LEGACY_ERROR_KEYWORDS.put("Forbidden", 403);
    LEGACY_ERROR_KEYWORDS.put("AccessDenied", 403);
    LEGACY_ERROR_KEYWORDS.put("NotFound", 404);
    LEGACY_ERROR_KEYWORDS.put("Throttling", 429);
    LEGACY_ERROR_KEYWORDS.put("RateLimit", 429);
    LEGACY_ERROR_KEYWORDS.put("InternalError", 500);
    LEGACY_ERROR_KEYWORDS.put("SystemError", 500);
  }

  /**
   * Resolve the appropriate HTTP status code for an API exception. Priority: 1) Body status_code,
   * 2) HTTP response status code, 3) Exact match in PublicErrorDef, 4) Keyword match for legacy
   * error codes, 5) Default to bodyStatusCode/httpStatusCode/200.
   *
   * <p>This method is null-safe: all parameters accept {@code null} values and will never cause
   * NullPointerException. Returns a primitive {@code int}, safe for direct use in builders.
   */
  private int resolveStatusCode(Integer bodyStatusCode, Integer httpStatusCode, String errorCode) {
    if (bodyStatusCode != null && bodyStatusCode != 200) {
      return bodyStatusCode;
    }
    if (httpStatusCode != null && httpStatusCode != 200) {
      return httpStatusCode;
    }
    if (errorCode != null) {
      // Exact match against PublicErrorDef
      for (PublicErrorDef def : PublicErrorDef.values()) {
        if (def.getErrorCode().equals(errorCode)) {
          return def.getStatusCode();
        }
      }
      // Fallback: exact match first, then keyword match for non-standard / legacy error codes
      if (LEGACY_ERROR_KEYWORDS.containsKey(errorCode)) {
        return LEGACY_ERROR_KEYWORDS.get(errorCode);
      }
      for (Map.Entry<String, Integer> entry : LEGACY_ERROR_KEYWORDS.entrySet()) {
        if (errorCode.contains(entry.getKey())) {
          return entry.getValue();
        }
      }
    }
    return bodyStatusCode != null && bodyStatusCode != 200
        ? bodyStatusCode
        : (httpStatusCode != null && httpStatusCode != 200 ? httpStatusCode : 400);
  }
}
