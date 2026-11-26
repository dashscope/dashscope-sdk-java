// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.chat;

import com.alibaba.dashscope.DashScopeAsyncClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.completions.Delta;
import com.alibaba.dashscope.types.messages.Message;
import io.reactivex.Flowable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Async chat completions: {@link DashScopeAsyncClient} mirrors the sync resource tree, returning
 * {@link CompletableFuture} for unary calls and RxJava {@link Flowable} for streaming (2.x parity).
 *
 * <p>Work runs on a daemon executor owned by the client; try-with-resources shuts it down. Compare
 * with the sync {@link ChatCompletionsCreate}.
 */
public final class ChatCompletionsCreateAsync {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen3.7-plus");
    List<Message> messages =
        Arrays.asList(Message.system("You are a helpful assistant."), Message.user("请用一句话介绍杭州"));
    ChatCompletionCreateParams params =
        ChatCompletionCreateParams.builder().model(model).messages(messages).build();

    try (DashScopeAsyncClient client = ExampleSupport.asyncClient()) {
      // Non-streaming: the call returns immediately; compose on the future.
      System.out.println("----- async non-streaming request -----");
      CompletableFuture<ChatCompletion> future = client.chat().completions().create(params);
      CompletableFuture<String> answer =
          future.thenApply(
              resp -> {
                System.out.println(resp.outputText());
                System.out.println(
                    "usage: "
                        + resp.getUsage().getTotalTokens()
                        + " tokens, request_id: "
                        + resp.getRequestId());
                return resp.outputText();
              });
      answer.join();

      // Streaming: chunks surface as a Flowable; blockingForEach consumes them on this thread.
      System.out.println("----- async streaming request -----");
      Flowable<ChatCompletionChunk> stream = client.chat().completions().createStreaming(params);
      stream.blockingForEach(
          chunk -> {
            Delta delta = chunk.delta();
            if (delta != null && delta.getContent() != null) {
              System.out.print(delta.getContent());
              System.out.flush();
            }
          });
      System.out.println();
    }
  }
}