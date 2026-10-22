// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.v3.resources.chat;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.FlowPublishers;
import com.alibaba.dashscope.v3.types.completions.ChatCompletion;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

/** Async variant of {@link ChatCompletionsService}. */
public final class ChatCompletionsAsyncService {

  private final ChatCompletionsService sync;
  private final Executor executor;

  public ChatCompletionsAsyncService(ChatCompletionsService sync, Executor executor) {
    this.sync = sync;
    this.executor = executor;
  }

  public CompletableFuture<ChatCompletion> create(ChatCompletionCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public CompletableFuture<ChatCompletion> create(
      ChatCompletionCreateParams params, RequestOptions options) {
    return CompletableFuture.supplyAsync(() -> sync.create(params, options), executor);
  }

  public Flow.Publisher<ChatCompletionChunk> createStreaming(ChatCompletionCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public Flow.Publisher<ChatCompletionChunk> createStreaming(
      ChatCompletionCreateParams params, RequestOptions options) {
    return FlowPublishers.fromStream(() -> sync.createStreaming(params, options), executor);
  }
}
