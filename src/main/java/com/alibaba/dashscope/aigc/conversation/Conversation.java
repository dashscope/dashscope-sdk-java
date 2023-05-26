// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.aigc.conversation;

import static com.alibaba.dashscope.utils.ApiKeywords.EVENT;

import com.alibaba.dashscope.BaseConversation;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.*;
import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Conversation implements BaseConversation {

  @Setter private long responseTimeout = 30;

  @Getter private final List<ChatMessage> history = Lists.newCopyOnWriteArrayList();

  private final Protocol protocol;

  public Conversation() {
    protocol = Protocol.HTTP;
  }

  public Conversation(String protocol) {
    this.protocol = Protocol.of(protocol);
  }

  /**
   * Call the server to get the result in the callback function.
   *
   * @param param The input param of class `ConversationParam`.
   * @param callback The callback to receive response, the template class is `ConversationResult`.
   */
  @Override
  public void call(ConversationParam param, ResultCallback<ConversationResult> callback) {
    final AtomicReference<ConversationResult> lastResult = new AtomicReference<>();

    class ReactCallback extends ResultCallback<Response> {

      public void onOpen(Status status) {
        callback.onOpen(status);
      }

      @Override
      public void onEvent(Response message) {
        try {
          ConversationResult result =
              (ConversationResult) ServiceFacility.prepareResult(protocol, param, message);
          if (protocol == Protocol.HTTP
              || !WebSocketEventType.TASK_STARTED
                  .getValue()
                  .equals(result.getHeaders().get(EVENT))) {
            lastResult.set(result);
            callback.onEvent(result);
          }
        } catch (Exception e) {
          callback.onError(new ApiException(e));
        }
      }

      @Override
      public void onComplete() {
        if (lastResult.get() != null) {
          history.add(
              ChatMessage.builder().role(Role.USER.getValue()).payload(param.getPrompt()).build());
          history.add(
              ChatMessage.builder()
                  .role(Role.BOT.getValue())
                  .payload(lastResult.get().getMessage().getPayload())
                  .build());
        }
        callback.onComplete();
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

    ServiceFacility.streamingOutWithCallback(
        protocol,
        ServiceFacility.prepareUrl(protocol, param),
        ServiceFacility.prepareHeaders(protocol, param),
        ServiceFacility.prepareRequest(protocol, param, WebSocketEventType.RUN_TASK),
        HttpMethod.POST,
        param.getMode(),
        new ReactCallback());
  }

  /**
   * Call the server to get the result by stream.
   *
   * @param param The input param of class `ConversationParam`.
   * @return A `Flowable` of the output structure.
   */
  @Override
  public Flowable<ConversationResult> streamCall(ConversationParam param) {
    final AtomicReference<ConversationResult> lastResult = new AtomicReference<>();
    if (param.getHistory() == null) {
      param.setHistory(history);
    }
    param.setMode(StreamingMode.OUT);
    return ServiceFacility.streamCall(protocol, null, param, responseTimeout)
        .map(
            message -> {
              lastResult.set((ConversationResult) message);
              return (ConversationResult) message;
            })
        .doOnComplete(
            () -> {
              if (lastResult.get() != null) {
                history.add(
                    ChatMessage.builder()
                        .role(Role.USER.getValue())
                        .payload(param.getPrompt())
                        .build());
                history.add(
                    ChatMessage.builder()
                        .role(Role.BOT.getValue())
                        .payload(
                            lastResult.get().getMessage() == null
                                ? null
                                : lastResult.get().getMessage().getPayload())
                        .build());
              }
            });
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `ConversationParam`.
   * @return The output structure of `QWenConversationResult`.
   */
  @Override
  public ConversationResult call(ConversationParam param) throws ApiException {
    try {
      if (param.getHistory() == null && !history.isEmpty()) {
        param.setHistory(history);
      }
      param.setMode(StreamingMode.NONE);
      ConversationResult result =
          (ConversationResult)
              ServiceFacility.call(protocol, null, param, HttpMethod.POST, responseTimeout);
      history.add(
          ChatMessage.builder().role(Role.USER.getValue()).payload(param.getPrompt()).build());
      history.add(
          ChatMessage.builder()
              .role(Role.BOT.getValue())
              .payload(result.getMessage().getPayload())
              .build());
      return result;
    } catch (Exception e) {
      throw e instanceof ApiException ? (ApiException) e : new ApiException(e);
    }
  }
}
