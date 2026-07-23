// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.alibaba.dashscope.utils.JsonUtils;
import com.alibaba.dashscope.utils.StringUtils;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        if (jsonObject.has(ApiKeywords.HEADER)) {
          JsonObject headers = jsonObject.get(ApiKeywords.HEADER).getAsJsonObject();
          if (headers.has(ApiKeywords.TASKID)) {
            this.setRequestId(headers.get(ApiKeywords.TASKID).getAsString());
          }
          // Extract status_code, code and message from header
          if (headers.has(ApiKeywords.STATUS_CODE)) {
            this.setStatusCode(
                headers.get(ApiKeywords.STATUS_CODE).isJsonNull()
                    ? null
                    : headers.get(ApiKeywords.STATUS_CODE).getAsInt());
          } else {
            // Set default status code
            this.setStatusCode(200);
          }
          if (headers.has(ApiKeywords.ERROR_CODE)) {
            this.setCode(
                headers.get(ApiKeywords.ERROR_CODE).isJsonNull()
                    ? ""
                    : headers.get(ApiKeywords.ERROR_CODE).getAsString());
          } else {
            // Set default empty string for successful responses
            this.setCode("");
          }
          if (headers.has(ApiKeywords.ERROR_MESSAGE)) {
            this.setMessage(
                headers.get(ApiKeywords.ERROR_MESSAGE).isJsonNull()
                    ? ""
                    : headers.get(ApiKeywords.ERROR_MESSAGE).getAsString());
          } else {
            // Set default empty string for successful responses
            this.setMessage("");
          }
          String errorCode = this.getCode();
          if (errorCode != null && !errorCode.isEmpty()) {
            int statusCode =
                resolveStatusCode(this.getStatusCode(), response.getHttpStatusCode(), errorCode);
            throw new ApiException(
                Status.builder()
                    .statusCode(statusCode > 0 ? statusCode : 500)
                    .code(errorCode)
                    .message(this.getMessage() != null ? this.getMessage() : "Unknown error")
                    .requestId(this.getRequestId())
                    .build());
          } else if (response.getHttpStatusCode() >= 400) {
            throw new ApiException(
                Status.builder()
                    .statusCode(PublicErrorDef.INTERNAL_ERROR.getStatusCode())
                    .code(PublicErrorDef.INTERNAL_ERROR.getErrorCode())
                    .message(
                        StringUtils.format(
                            "%s [http_status=%d, original_message=%s]",
                            PublicErrorDef.INTERNAL_ERROR.getErrorMsg(),
                            response.getHttpStatusCode(),
                            (response.getMessage() != null ? response.getMessage() : "No message")))
                    .requestId(this.getRequestId())
                    .build());
          }
        }
        if (jsonObject.has(ApiKeywords.PAYLOAD)) {
          JsonObject payload = jsonObject.getAsJsonObject(ApiKeywords.PAYLOAD);
          if (payload.has(ApiKeywords.OUTPUT)) {
            this.output =
                payload.get(ApiKeywords.OUTPUT).isJsonNull()
                    ? null
                    : payload.get(ApiKeywords.OUTPUT);
          }
          if (payload.has(ApiKeywords.USAGE)) {
            this.setUsage(
                payload.get(ApiKeywords.USAGE).isJsonNull()
                    ? null
                    : payload.get(ApiKeywords.USAGE));
          }
        }
      } else {
        this.output = response.getBinary();
      }
    } else {
      JsonObject jsonObject = JsonUtils.parse(response.getMessage());
      // Set HTTP status code if available
      if (response.getHttpStatusCode() != null) {
        this.setStatusCode(response.getHttpStatusCode());
      }
      if (jsonObject.has(ApiKeywords.OUTPUT)) {
        this.output =
            jsonObject.get(ApiKeywords.OUTPUT).isJsonNull()
                ? null
                : jsonObject.get(ApiKeywords.OUTPUT).getAsJsonObject();
      }
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
      if (jsonObject.has(ApiKeywords.CODE)) {
        this.setCode(
            jsonObject.get(ApiKeywords.CODE).isJsonNull()
                ? ""
                : jsonObject.get(ApiKeywords.CODE).getAsString());
      } else {
        // Set default empty string for successful responses
        this.setCode("");
      }
      if (jsonObject.has(ApiKeywords.MESSAGE)) {
        this.setMessage(
            jsonObject.get(ApiKeywords.MESSAGE).isJsonNull()
                ? ""
                : jsonObject.get(ApiKeywords.MESSAGE).getAsString());
      } else {
        // Set default empty string for successful responses
        this.setMessage("");
      }
      if (this.getCode() != null && !this.getCode().isEmpty()) {
        int resolvedStatusCode =
            resolveStatusCode(this.getStatusCode(), response.getHttpStatusCode(), this.getCode());
        throw new ApiException(
            Status.builder()
                .statusCode(resolvedStatusCode)
                .code(this.getCode())
                .message(this.getMessage())
                .requestId(this.getRequestId())
                .build());
      }
      if (jsonObject.has(ApiKeywords.DATA)) {
        jsonObject.remove(ApiKeywords.REQUEST_ID);
        this.output = jsonObject;
      }
    }
    return (T) this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult) throws ApiException {
    if (!isFlattenResult) {
      return fromResponse(protocol, response);
    } else {
      this.setHeaders(changeHeaders(response.getHeaders()));
      // flatten not support websocket.
      if (protocol == Protocol.WEBSOCKET) {
        if (response.getBinary() == null) {
          JsonObject jsonObject = JsonUtils.parse(response.getMessage());
          this.output = jsonObject;
          // convert to the result
        } else {
          this.output = response.getBinary();
        }
      } else { // HTTP
        JsonObject jsonObject = JsonUtils.parse(response.getMessage());
        this.output = jsonObject;
        this.event = response.getEvent();
      }
      return (T) this;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Result> T fromResponse(
      Protocol protocol, NetworkResponse response, boolean isFlattenResult, HalfDuplexRequest req)
      throws ApiException {
    this.setHeaders(changeHeaders(response.getHeaders()));
    // check it's encrypted output
    if ((response.getHeaders().containsKey("X-DashScope-OutputEncrypted".toLowerCase())
            || req.isEncryptRequest())
        && protocol == Protocol.HTTP) {
      // Set HTTP status code if available
      if (response.getHttpStatusCode() != null) {
        this.setStatusCode(response.getHttpStatusCode());
      }
      JsonObject jsonObject = JsonUtils.parse(response.getMessage());
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
        this.output = JsonUtils.parse(plainOutput);
      } else {
        this.output = null;
      }
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
      if (jsonObject.has(ApiKeywords.CODE)) {
        this.setCode(
            jsonObject.get(ApiKeywords.CODE).isJsonNull()
                ? ""
                : jsonObject.get(ApiKeywords.CODE).getAsString());
      } else {
        // Set default empty string for successful responses
        this.setCode("");
      }
      if (jsonObject.has(ApiKeywords.MESSAGE)) {
        this.setMessage(
            jsonObject.get(ApiKeywords.MESSAGE).isJsonNull()
                ? ""
                : jsonObject.get(ApiKeywords.MESSAGE).getAsString());
      } else {
        // Set default empty string for successful responses
        this.setMessage("");
      }
      if (this.getCode() != null && !this.getCode().isEmpty()) {
        int resolvedStatusCode =
            resolveStatusCode(this.getStatusCode(), response.getHttpStatusCode(), this.getCode());
        throw new ApiException(
            Status.builder()
                .statusCode(resolvedStatusCode)
                .code(this.getCode())
                .message(this.getMessage())
                .requestId(this.getRequestId())
                .build());
      }
      if (jsonObject.has(ApiKeywords.DATA)) {
        if (jsonObject.has(ApiKeywords.REQUEST_ID)) {
          jsonObject.remove(ApiKeywords.REQUEST_ID);
        }
      }
      return (T) this;
    }
    return fromResponse(protocol, response, isFlattenResult);
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
    return bodyStatusCode != null
        ? bodyStatusCode
        : (httpStatusCode != null ? httpStatusCode : 200);
  }
}
