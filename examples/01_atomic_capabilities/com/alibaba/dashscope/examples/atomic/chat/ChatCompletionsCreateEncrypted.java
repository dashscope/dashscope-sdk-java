// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.chat;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import java.util.Arrays;

/**
 * Encrypted chat channel (J7): {@code RequestOptions.enableEncryption(true)} fetches the server's
 * latest RSA public key ({@code GET /public-keys/latest}), wraps a fresh per-request AES key with
 * it, AES-GCM-encrypts the sensitive {@code input} part, and decrypts the encrypted {@code output}
 * transparently. The call shape is identical to {@link ChatCompletionsCreate}.
 *
 * <p>Encryption is per-request: the same client can mix plain and encrypted calls.
 */
public final class ChatCompletionsCreateEncrypted {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen3.7-plus");

    try (DashScopeClient client = ExampleSupport.client()) {
      System.out.println("----- encrypted request -----");
      ChatCompletion resp =
          client
              .chat()
              .completions()
              .create(
                  ChatCompletionCreateParams.builder()
                      .model(model)
                      .messages(
                          Arrays.asList(
                              Message.system("You are a helpful assistant."),
                              Message.user("请用一句话介绍杭州")))
                      .build(),
                  RequestOptions.builder().enableEncryption(true).build());
      // outputText() is already decrypted by the SDK.
      System.out.println(resp.outputText());
      System.out.println("request_id: " + resp.getRequestId());
    }
  }
}