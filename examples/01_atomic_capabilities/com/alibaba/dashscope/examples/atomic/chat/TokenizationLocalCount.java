// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.chat;

import com.alibaba.dashscope.tokenization.Tokenizer;
import com.alibaba.dashscope.tokenization.TokenizerFactory;
import java.util.List;

/**
 * Local Qwen BPE tokenizer: encode/decode entirely offline, no API key and no network call (the
 * vocabulary {@code qwen.tiktoken} is resolved from {@code DASHSCOPE_QWEN_VOCAB}, the classpath, a
 * verified local cache, or a one-time ModelScope download).
 *
 * <p>For server-side counting see {@link TokenizationCount}. Local results are approximate: they
 * are not guaranteed to match the model's own tokenization exactly.
 */
public final class TokenizationLocalCount {

  public static void main(String[] args) {
    Tokenizer tokenizer = TokenizerFactory.qwen();

    // Ordinary encoding: special tokens in the text are treated as plain text.
    System.out.println("----- encodeOrdinary -----");
    String text = "你好，杭州！";
    List<Integer> ids = tokenizer.encodeOrdinary(text);
    System.out.println("text: " + text);
    System.out.println("token count: " + ids.size());
    System.out.println("token_ids: " + ids);
    System.out.println("decoded: " + tokenizer.decode(ids));

    // Special-token-aware encoding: "all" recognizes <|im_start|> / <|im_end|> etc. as single
    // tokens; "none" forbids them, "none_raise" throws when the input has none.
    System.out.println("----- encode (special tokens) -----");
    String chatText = "<|im_start|>user\n你好<|im_end|>";
    List<Integer> chatIds = tokenizer.encode(chatText, "all");
    System.out.println("text: " + chatText.replace("\n", "\\n"));
    System.out.println("token count: " + chatIds.size());
    System.out.println("decoded: " + tokenizer.decode(chatIds).replace("\n", "\\n"));
  }
}