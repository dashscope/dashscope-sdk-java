// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.resource;

import com.alibaba.dashscope.errors.ApiConnectionException;
import com.alibaba.dashscope.errors.ApiTimeoutException;
import com.alibaba.dashscope.errors.DashScopeException;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.errors.SdkErrorCode;
import com.alibaba.dashscope.errors.SdkException;
import com.alibaba.dashscope.legacy_2x.common.Status;
import com.alibaba.dashscope.legacy_2x.exception.ApiException;
import com.alibaba.dashscope.legacy_2x.exception.NoApiKeyException;
import java.io.IOException;
import java.net.SocketTimeoutException;

/** Single boundary translating the 2.x error style into the 3.0 errors family. */
final class AgentStudioErrors {
  private AgentStudioErrors() {}

  static RuntimeException translate(Throwable e) {
    if (e instanceof DashScopeException || e instanceof IllegalArgumentException) {
      return (RuntimeException) e;
    }
    if (e instanceof ApiException) {
      return fromApiException((ApiException) e);
    }
    if (e instanceof NoApiKeyException) {
      return Errors.fromSdkErrorCode(SdkErrorCode.SDK_INVALID_API_KEY.getCode(), e.getMessage(), e);
    }
    if (e instanceof IOException) {
      return network(e);
    }
    return new SdkException(String.valueOf(e.getMessage()), e.getClass().getName(), null, false, e);
  }

  static RuntimeException network(Throwable e) {
    String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    if (e instanceof SocketTimeoutException) {
      return new ApiTimeoutException(
          detail, SdkErrorCode.SDK_AGENTSTUDIO_API_TIMEOUT_ERROR.getCode(), e);
    }
    return new ApiConnectionException(
        detail, SdkErrorCode.SDK_AGENTSTUDIO_API_CONNECTION_ERROR.getCode(), e);
  }

  private static RuntimeException fromApiException(ApiException e) {
    Status status = e.getStatus();
    if (status == null) {
      return new SdkException(
          String.valueOf(e.getMessage()), e.getClass().getName(), null, false, e);
    }
    int statusCode = status.getStatusCode();
    String code = status.getCode();
    String message = status.getMessage();
    String requestId = status.getRequestId();
    if (statusCode == 200) {
      return Errors.fromBusinessCode(code, message, requestId, null, null);
    }
    if (statusCode > 0) {
      return Errors.fromStatus(statusCode, code, message, requestId, null, null);
    }
    if ("stream_timeout".equals(code)) {
      return new ApiTimeoutException(
          message, SdkErrorCode.SDK_AGENTSTUDIO_API_TIMEOUT_ERROR.getCode(), e);
    }
    if (code == null || "network error".equals(code)) {
      return network(e.getCause() != null ? e.getCause() : e);
    }
    return Errors.fromSdkErrorCode(code, message, e);
  }
}
