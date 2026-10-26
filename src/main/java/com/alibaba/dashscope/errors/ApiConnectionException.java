// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

/** Network-level failure (connection refused, DNS, TLS, reset mid-stream...). */
public class ApiConnectionException extends DashScopeException {
  public ApiConnectionException(String message, Throwable cause) {
    super(message, cause);
  }

  public ApiConnectionException(String message, String code, Throwable cause) {
    super(message, code, null, null, null, null, cause);
  }
}
