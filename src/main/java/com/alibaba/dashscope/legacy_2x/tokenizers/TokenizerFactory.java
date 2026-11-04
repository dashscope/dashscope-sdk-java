package com.alibaba.dashscope.legacy_2x.tokenizers;

@Deprecated
public final class TokenizerFactory {
  public static Tokenizer qwen() {
    return new QwenTokenizer();
  }
}
