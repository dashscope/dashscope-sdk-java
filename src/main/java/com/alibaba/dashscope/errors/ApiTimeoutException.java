// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.errors;

/** Request timed out (connect/read/write). */
public class ApiTimeoutException extends ApiConnectionException {
  public ApiTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
