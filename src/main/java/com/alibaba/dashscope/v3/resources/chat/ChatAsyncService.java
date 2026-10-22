// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.v3.resources.chat;

import java.util.concurrent.Executor;

/** Entry of the async chat domain. */
public final class ChatAsyncService {

  private final ChatCompletionsAsyncService completions;

  public ChatAsyncService(ChatCompletionsAsyncService completions) {
    this.completions = completions;
  }

  public ChatAsyncService(ChatService sync, Executor executor) {
    this(new ChatCompletionsAsyncService(sync.completions(), executor));
  }

  public ChatCompletionsAsyncService completions() {
    return completions;
  }
}
