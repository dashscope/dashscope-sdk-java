// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.conversation.Conversation;
import com.alibaba.dashscope.conversation.ConversationResult;
import com.alibaba.dashscope.conversation.EventType;
import com.alibaba.dashscope.conversation.qwen.QWenConversationParam;
import com.alibaba.dashscope.mock.MockServer;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import java.util.List;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestConversation {

  String[] protocols = {Protocol.HTTP.getValue(), Protocol.WEBSOCKET.getValue()};

  private final MockServer mockServer = new MockServer();

  private String oldBaseHttpApiUrl = null;

  private String oldBaseWebsocketApiUrl = null;

  @BeforeEach
  public void setUp() {
    oldBaseHttpApiUrl = Constants.baseHttpApiUrl;
    oldBaseWebsocketApiUrl = Constants.baseWebsocketApiUrl;
    Constants.baseHttpApiUrl = "http://localhost:8080/api/v1";
    Constants.baseWebsocketApiUrl = "ws://localhost:8080/api-ws/v1/inference";
    mockServer.start(0, "testKey");
  }

  @AfterEach
  public void tearDown() {
    Constants.baseHttpApiUrl = oldBaseHttpApiUrl;
    Constants.baseWebsocketApiUrl = oldBaseWebsocketApiUrl;
    mockServer.tearDown();
  }

  @Test
  public void testBasicConversation() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .maxLength(10)
              .apiKey("testKey")
              .msgId("test-id")
              .topP(0.8)
              .build();

      Flowable<ConversationResult> flowable = conversation.streamCall(param);
      flowable.blockingForEach(
          msg -> {
            assert ("test-id".equals(msg.getMsgId()));
          });
      assert (conversation.getHistory().size() == 2);
    }
  }

  @Test
  public void testBlockingConversation() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder()
              .model(QWenConversationParam.QWEN_V1)
              .prompt("hello")
              .apiKey("testKey")
              .build();

      ConversationResult result = conversation.call(param);
      log.info(JsonUtils.toJson(result));
      assert (conversation.getHistory().size() == 2);
    }
  }

  @Test
  public void testConversationWithCallback() throws InterruptedException {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder()
              .model(QWenConversationParam.QWEN_V1)
              .prompt("hello")
              .apiKey("testKey")
              .build();

      final List<Object> ret = Lists.newArrayList();
      Semaphore semaphore = new Semaphore(0);

      class ReactCallback extends ResultCallback<ConversationResult> {

        @Override
        public void onEvent(String eventType, ConversationResult message) {
          if (eventType.equals("task-finished")) {
            ret.add(message);
            semaphore.release();
          }
        }

        public void onError(Exception e) {
          throw new RuntimeException(e);
        }
      }
      conversation.call(param, new ReactCallback());
      semaphore.acquire();
      assert (conversation.getHistory().size() == 2);
      log.info(JsonUtils.toJson(ret));
    }
  }

  @Test
  public void testStreamingConversationWithCallback() throws InterruptedException {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .apiKey("testKey")
              .build();

      final List<Object> ret = Lists.newArrayList();
      Semaphore semaphore = new Semaphore(0);
      class ReactCallback extends ResultCallback<ConversationResult> {

        @Override
        public void onEvent(String eventType, ConversationResult message) {
          if (EventType.RESULT_GENERATED.getValue().equals(eventType)) {
            ret.add(message);
          }
          if (EventType.TASK_FINISHED.getValue().equals(eventType)) {
            ret.add(message);
            semaphore.release();
          }
        }

        public void onError(Exception e) {
          throw new RuntimeException(e);
        }
      }
      conversation.call(param, new ReactCallback());
      semaphore.acquire();
      assert (conversation.getHistory().size() == 2);
      log.info(JsonUtils.toJson(ret));
    }
  }

  @Test
  public void testMultiRoundConversation() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .apiKey("testKey")
              .build();

      Flowable<ConversationResult> flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
      assert (conversation.getHistory().size() == 2);

      param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1)
              .prompt("May I ask you some questions?").stream(true)
              .apiKey("testKey")
              .build();

      flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
      assert (conversation.getHistory().size() == 4);

      param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1)
              .prompt("My name is Siri").stream(true)
              .apiKey("testKey")
              .build();

      flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
      assert (conversation.getHistory().size() == 6);

      param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1)
              .prompt("What is my name?").stream(true)
              .apiKey("testKey")
              .build();

      flowable = conversation.streamCall(param);
      List<ConversationResult> ret = Lists.newArrayList();
      flowable.blockingForEach(ret::add);
      assert (conversation.getHistory().size() == 8);
      log.info(JsonUtils.toJson(ret));
      assert (ret.get(ret.size() - 1).getMessage().getPayload().contains("Siri"));
    }
  }
}
