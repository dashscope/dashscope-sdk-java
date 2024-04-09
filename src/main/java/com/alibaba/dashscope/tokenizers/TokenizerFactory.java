package com.alibaba.dashscope.tokenizers;

public final class TokenizerFactory {
  public static Tokenizer qwen() {
    return new QwenTokenizer();
  }
}
