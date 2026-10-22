// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.chat;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.alibaba.dashscope.v3.types.tokenization.TokenCount;
import com.alibaba.dashscope.v3.types.tokenization.TokenizationCountParams;
import java.util.Arrays;

/**
 * Server-side token counting over a plain prompt and over a chat message list. Translation of
 * the Python blueprint example 01_atomic_capabilities/text_generation/tokenization.py.
 *
 * <p>For the local BPE tokenizer (no network) see {@code
 * com.alibaba.dashscope.v3.tokenization.TokenizerFactory}.
 */
public final class TokenizationCount {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen3.7-plus");

    try (DashScopeClient client = ExampleSupport.client()) {
      // Count the tokens of a plain prompt
      System.out.println("----- count (prompt) -----");
      TokenCount resp =
          client
              .tokenization()
              .count(
                  TokenizationCountParams.builder()
                      .model(model)
                      .prompt("请用一句话介绍杭州")
                      .build());
      System.out.println("input_tokens: " + resp.getInputTokens());
      System.out.println("token_ids: " + resp.getTokenIds());

      // Count the tokens of a chat message list
      System.out.println("----- count (messages) -----");
      resp =
          client
              .tokenization()
              .count(
                  TokenizationCountParams.builder()
                      .model(model)
                      .messages(
                          Arrays.asList(
                              Message.system("You are a helpful assistant."),
                              Message.user("请用一句话介绍杭州")))
                      .build());
      System.out.println("input_tokens: " + resp.getInputTokens());
    }
  }
}
