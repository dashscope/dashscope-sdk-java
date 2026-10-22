// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.tokenization;

/** {@code allowedSpecial} was not one of "all"/"none"/"none_raise" (2.x {@code UnSupportedSpecialTokenMode}). */
public final class UnsupportedSpecialTokenModeException extends TokenizerException {
  public UnsupportedSpecialTokenModeException(String message) {
    super(message);
  }
}
