// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.tokenization;

/** {@code encode(text, "none_raise")} encountered no special token (2.x {@code NoSpecialTokenExists}). */
public final class NoSpecialTokenException extends TokenizerException {
  public NoSpecialTokenException(String message) {
    super(message);
  }
}
