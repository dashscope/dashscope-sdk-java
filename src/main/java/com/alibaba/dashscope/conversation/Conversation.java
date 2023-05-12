// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.conversation;

import com.alibaba.dashscope.BaseConversation;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.conversation.rpc.ConversationHttpRpc;
import com.alibaba.dashscope.conversation.rpc.ConversationRpc;
import com.alibaba.dashscope.conversation.rpc.ConversationWebsocketRpc;
import com.google.common.collect.Lists;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Conversation implements BaseConversation {

  private final ConversationRpc conversationRpc;

  @Setter private long observerTimeout = 30;

  @Getter private final List<ChatMessage> history = Lists.newArrayList();

  public Conversation() {
    conversationRpc = new ConversationHttpRpc();
  }

  public Conversation(String protocol) {
    if (Protocol.WEBSOCKET.getValue().equals(protocol)) {
      conversationRpc = new ConversationWebsocketRpc();
    } else {
      conversationRpc = new ConversationHttpRpc();
    }
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `ConversationParam`.
   * @param callback The callback to receive response, the template class is `ConversationResult`.
   */
  @Override
  public void call(ConversationParam param, ResultCallback<ConversationResult> callback) {
    class ReactCallback extends ResultCallback<ConversationResult> {

      public void onOpen(Status status) {
        callback.onOpen(status);
      }

      @Override
      public void onEvent(String type, ConversationResult message) {
        if (EventType.TASK_FINISHED.getValue().equals(type) && (message != null)) {
          history.add(
              ChatMessage.builder().role(Role.USER.getValue()).payload(param.getPrompt()).build());
          history.add(
              ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(message.getMessage().getPayload())
                  .build());
        }
        callback.onEvent(type, message);
      }

      public void onClose(Status status) {
        callback.onClose(status);
      }

      @Override
      public void onError(Exception e) {
        callback.onError(e);
      }

      public void doClose(Status status) {
        callback.doClose(status);
      }
    }
    if (param.getHistory() == null) {
      param.setHistory(history);
    }
    conversationRpc.call(param, new ReactCallback());
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `ConversationParam`.
   * @return A `Flowable` of the output structure.
   */
  @Override
  public Flowable<ConversationResult> streamCall(ConversationParam param) {
    class ReactCallback extends ResultCallback<ConversationResult> {

      private final FlowableEmitter<ConversationResult> emitter;

      public ReactCallback(FlowableEmitter<ConversationResult> emitter) {
        this.emitter = emitter;
      }

      @Override
      public void onEvent(String type, ConversationResult message) {
        if (EventType.RESULT_GENERATED.getValue().equals(type) && param.isStream()) {
          this.emitter.onNext(message);
        } else if (EventType.TASK_FINISHED.getValue().equals(type) && (message != null)) {
          history.add(
              ChatMessage.builder().role(Role.USER.getValue()).payload(param.getPrompt()).build());
          history.add(
              ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(message.getMessage() == null ? null : message.getMessage().getPayload())
                  .build());
          this.emitter.onNext(message);
          this.emitter.onComplete();
        }
      }

      @Override
      public void onError(Exception e) {
        this.emitter.onError(e);
      }
    }

    if (param.getHistory() == null) {
      param.setHistory(history);
    }
    return Flowable.<ConversationResult>create(
            emitter -> conversationRpc.call(param, new ReactCallback(emitter)),
            BackpressureStrategy.BUFFER)
        .timeout(observerTimeout, TimeUnit.SECONDS);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   */
  @Override
  public ConversationResult call(ConversationParam param) {

    class ReactCallback extends ResultCallback<ConversationResult> {

      private final FlowableEmitter<ConversationResult> emitter;

      public ReactCallback(FlowableEmitter<ConversationResult> emitter) {
        this.emitter = emitter;
      }

      @Override
      public void onEvent(String type, ConversationResult message) {
        if (EventType.TASK_FINISHED.getValue().equals(type)) {
          this.emitter.onNext(message);
          this.emitter.onComplete();
        }
      }

      @Override
      public void onError(Exception e) {
        this.emitter.onError(e);
      }
    }

    if (param.getHistory() == null && !history.isEmpty()) {
      param.setHistory(history);
    }
    Flowable<ConversationResult> flowable =
        Flowable.<ConversationResult>create(
                emitter -> conversationRpc.call(param, new ReactCallback(emitter)),
                BackpressureStrategy.BUFFER)
            .timeout(observerTimeout, TimeUnit.SECONDS);
    ConversationResult result = flowable.blockingFirst();
    if (result != null) {
      history.add(
          ChatMessage.builder().role(Role.USER.getValue()).payload(param.getPrompt()).build());
      history.add(
          ChatMessage.builder()
              .role(Role.BOT.getValue())
              .payload(result.getMessage().getPayload())
              .build());
    }
    return result;
  }
}
