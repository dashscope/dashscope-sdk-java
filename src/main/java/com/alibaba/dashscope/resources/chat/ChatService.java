// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.chat;

import com.alibaba.dashscope.transport.HttpTransport;

/** {@code chat} resource group. */
public final class ChatService {
  private final ChatCompletionsService completions;

  public ChatService(HttpTransport transport) {
    this.completions = new ChatCompletionsService(transport);
  }

  public ChatCompletionsService completions() {
    return completions;
  }
}
