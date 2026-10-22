// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.tokenization;

/** Base type for local tokenizer failures (3.0: unchecked, no transport involved). */
public class TokenizerException extends RuntimeException {
  public TokenizerException(String message) {
    super(message);
  }
}
