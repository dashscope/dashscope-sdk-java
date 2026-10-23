// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.v3.resources.chat;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.RxStreams;
import com.alibaba.dashscope.v3.types.completions.ChatCompletion;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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

  public Flowable<ChatCompletionChunk> createStreaming(ChatCompletionCreateParams params) {
    return createStreaming(params, RequestOptions.NONE);
  }

  public Flowable<ChatCompletionChunk> createStreaming(
      ChatCompletionCreateParams params, RequestOptions options) {
    return RxStreams.fromStream(() -> sync.createStreaming(params, options), executor);
  }
}
