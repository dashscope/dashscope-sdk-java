// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.exception;

/** 3.0 copy of the 2.x NoApiKeyException for the agentstudio subtree. */
public class NoApiKeyException extends Exception {
  public NoApiKeyException() {
    super("Can not find api-key.");
  }
}
