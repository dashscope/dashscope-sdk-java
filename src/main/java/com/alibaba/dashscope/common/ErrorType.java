// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

public enum ErrorType {

  /** The error happens in the response body. */
  RESPONSE_ERROR("response_error"),

  /** The error happens because the request is canceled. */
  REQUEST_CANCELLED("request_cancelled"),

  /** The error happens because the network protocol is not supported. */
  PROTOCOL_UNSUPPORTED("protocol_unsupported"),

  /** The api key is not correct. */
  API_KEY_ERROR("api_key_error"),

  /** An unknown error. */
  UNKNOWN_ERROR("unknown_error"),

  NETORK_ERROR("network error"),
  ;

  private final String value;

  private ErrorType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
