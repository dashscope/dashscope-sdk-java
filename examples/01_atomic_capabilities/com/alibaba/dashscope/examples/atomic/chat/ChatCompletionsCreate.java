// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.chat;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.common.ResponseStream;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.completions.Delta;
import com.alibaba.dashscope.types.messages.Message;
import java.util.Arrays;
import java.util.List;

/**
 * Chat completions: non-streaming and streaming. Translation of the Python blueprint example
 * 01_atomic_capabilities/text_generation/chat.py.
 *
 * <p>The Python module-level one-call quickstart {@code dashscope.chat(model, prompt)} has no Java
 * 3.0 equivalent; the try-with-resources {@link DashScopeClient} below is the entry point.
 */
public final class ChatCompletionsCreate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen3.7-plus");
    List<Message> messages =
        Arrays.asList(Message.system("You are a helpful assistant."), Message.user("请用一句话介绍杭州"));

    try (DashScopeClient client = ExampleSupport.client()) {
      // Non-streaming
      System.out.println("----- non-streaming request -----");
      ChatCompletion resp =
          client
              .chat()
              .completions()
              .create(ChatCompletionCreateParams.builder().model(model).messages(messages).build());
      System.out.println(resp.outputText());
      System.out.println(
          "usage: "
              + resp.getUsage().getTotalTokens()
              + " tokens, request_id: "
              + resp.getRequestId());

      // Streaming: each chunk's delta carries a content increment (the SDK asks the server for
      // incremental frames by default when streaming).
      System.out.println("----- streaming request -----");
      try (ResponseStream<ChatCompletionChunk> stream =
          client
              .chat()
              .completions()
              .createStreaming(
                  ChatCompletionCreateParams.builder().model(model).messages(messages).build())) {
        for (ChatCompletionChunk chunk : stream) {
          Delta delta = chunk.delta();
          if (delta != null && delta.getContent() != null) {
            System.out.print(delta.getContent());
            System.out.flush();
          }
        }
      }
      System.out.println();
    }
  }
}
