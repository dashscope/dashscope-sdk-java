// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.legacy_2x.exception;

import com.alibaba.dashscope.legacy_2x.utils.Constants;

@Deprecated
public class NoApiKeyException extends Exception {
  public NoApiKeyException() {
    super(Constants.NO_API_KEY_ERROR);
  }
}
