// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.chat;

import com.alibaba.dashscope.v3.transport.HttpTransport;

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
