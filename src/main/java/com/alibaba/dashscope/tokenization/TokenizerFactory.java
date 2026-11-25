// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.tokenization;

/** Local tokenizer factory (3.0 home; 2.x {@code tokenizers.TokenizerFactory} delegates here). */
public final class TokenizerFactory {
  private TokenizerFactory() {}

  public static Tokenizer qwen() {
    return new QwenTokenizer();
  }
}
