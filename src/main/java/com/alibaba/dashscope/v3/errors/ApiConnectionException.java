// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.errors;

/** Network-level failure (connection refused, DNS, TLS, reset mid-stream...). */
public class ApiConnectionException extends DashScopeException {
  public ApiConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
