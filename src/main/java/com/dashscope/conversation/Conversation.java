package com.dashscope.conversation;

import com.dashscope.BaseConversation;
import com.dashscope.common.Protocol;
import com.dashscope.common.ResultCallback;
import com.dashscope.common.Role;
import com.dashscope.common.Status;
import com.dashscope.conversation.rpc.ConversationHttpRpc;
import com.dashscope.conversation.rpc.ConversationRpc;
import com.dashscope.conversation.rpc.ConversationWebsocketRpc;
import com.google.common.collect.Lists;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Conversation implements BaseConversation {

  private final ConversationRpc conversationRpc;

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
   * @param callback The callback to receive response, the template class is
   *     `ConversationResultBase`.
   */
  @Override
  public void call(ConversationParamBase param, ResultCallback<ConversationResultBase> callback) {
    class ReactCallback extends ResultCallback<ConversationResultBase> {

      public void onOpen(Status status) {
        callback.onOpen(status);
      }

      @Override
      public void onEvent(String type, ConversationResultBase message) {
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
  public Flowable<ConversationResultBase> streamCall(ConversationParamBase param) {
    class ReactCallback extends ResultCallback<ConversationResultBase> {

      private final FlowableEmitter<ConversationResultBase> emitter;

      public ReactCallback(FlowableEmitter<ConversationResultBase> emitter) {
        this.emitter = emitter;
      }

      @Override
      public void onEvent(String type, ConversationResultBase message) {
        if (EventType.RESULT_GENERATED.getValue().equals(type) && param.isStream()) {
          this.emitter.onNext(message);
        } else if (EventType.TASK_FINISHED.getValue().equals(type) && (message != null)) {
          history.add(
              ChatMessage.builder().role(Role.USER.getValue()).payload(param.getPrompt()).build());
          history.add(
              ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(message.getMessage().getPayload())
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
    return Flowable.create(
        emitter -> conversationRpc.call(param, new ReactCallback(emitter)),
        BackpressureStrategy.BUFFER);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   */
  @Override
  public ConversationResultBase call(ConversationParamBase param) {

    class ReactCallback extends ResultCallback<ConversationResultBase> {

      private final FlowableEmitter<ConversationResultBase> emitter;

      public ReactCallback(FlowableEmitter<ConversationResultBase> emitter) {
        this.emitter = emitter;
      }

      @Override
      public void onEvent(String type, ConversationResultBase message) {
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
    Flowable<ConversationResultBase> flowable =
        Flowable.create(
            emitter -> conversationRpc.call(param, new ReactCallback(emitter)),
            BackpressureStrategy.BUFFER);
    ConversationResultBase result = flowable.blockingFirst();
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
