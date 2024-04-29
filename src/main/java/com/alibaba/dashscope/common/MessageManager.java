// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.common;

import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.utils.ApiKeywords;
import com.google.common.collect.EvictingQueue;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;

/**
 * Will be delete next release.
 *
 * @deprecated
 */
@Deprecated
public final class MessageManager {
  private static final int DEFAULT_MAXIMUM_MESSAGES = 10;
  private final EvictingQueue<Message> messages;

  public MessageManager() {
    messages = EvictingQueue.create(DEFAULT_MAXIMUM_MESSAGES);
  }

  public MessageManager(int maxMessages) {
    messages = EvictingQueue.create(maxMessages);
  }

  public void add(ConversationParam param) {}

  public void add(DashScopeResult result) {
    JsonObject obj = (JsonObject) result.getOutput();
    if (obj.has(ApiKeywords.TEXT)) {
      messages.add(
          Message.builder()
              .role(Role.ASSISTANT.getValue())
              .content(obj.get(ApiKeywords.TEXT).getAsString())
              .build());
    }
  }

  public void add(ConversationResult result) {
    addWithOutput(result.getOutput());
  }

  public void add(Message msg) {
    messages.add(msg);
  }

  /**
   * Get all the `Message`
   *
   * @return The `Message` list.
   */
  public List<Message> get() {
    return Arrays.asList(messages.toArray(new Message[0]));
  }

  /**
   * Get part of `Message`
   *
   * @param start The start index.
   * @param end The end index
   * @return The `Message` list.
   */
  public List<Message> get(int start, int end) {
    return Arrays.asList(messages.toArray(new Message[0])).subList(start, end);
  }

  public void add(GenerationResult result) {
    addWithOutput(result.getOutput());
  }

  private void addWithOutput(GenerationOutput output) {
    if (output.getChoices() != null && !output.getChoices().isEmpty()) {
      // the first is assistant response
      GenerationOutput.Choice choice = output.getChoices().get(0);
      messages.add(choice.getMessage());
    }

    if (output.getText() != null) {
      messages.add(
          Message.builder().role(Role.ASSISTANT.getValue()).content(output.getText()).build());
    }
  }
}
