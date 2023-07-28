// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.exception;

import com.alibaba.dashscope.utils.Constants;

public class NoApiKeyException extends Exception {
  public NoApiKeyException() {
    super(Constants.NO_API_KEY_ERROR);
  }
}
