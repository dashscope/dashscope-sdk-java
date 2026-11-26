// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.exception;

/** No API key could be resolved for the request. */
public class NoApiKeyException extends Exception {
  public NoApiKeyException() {
    super("Can not find api-key.");
  }
}
