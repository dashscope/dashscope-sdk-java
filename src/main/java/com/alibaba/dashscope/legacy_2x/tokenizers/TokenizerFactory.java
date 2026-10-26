package com.alibaba.dashscope.legacy_2x.tokenizers;

public final class TokenizerFactory {
  public static Tokenizer qwen() {
    return new QwenTokenizer();
  }
}
