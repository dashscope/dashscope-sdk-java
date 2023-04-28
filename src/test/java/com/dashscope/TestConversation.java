package com.dashscope;

import com.alibaba.fastjson.JSON;
import com.dashscope.common.Protocol;
import com.dashscope.common.ResultCallback;
import com.dashscope.conversation.Conversation;
import com.dashscope.conversation.ConversationResultBase;
import com.dashscope.conversation.EventType;
import com.dashscope.conversation.qwen.QWenConversationParam;
import com.google.common.collect.Lists;
import io.reactivex.Flowable;
import java.util.List;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TestConversation {

  String[] protocols = {Protocol.HTTP.getValue(), Protocol.WEBSOCKET.getValue()};

  @Test
  public void testBasicConversation() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .maxLength(10)
              .topP(0.8)
              .build();

      Flowable<ConversationResultBase> flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JSON.toJSONString(msg)));
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
              .build();

      ConversationResultBase result = conversation.call(param);
      log.info(JSON.toJSONString(result));
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
              .build();

      final List<Object> ret = Lists.newArrayList();
      Semaphore semaphore = new Semaphore(0);

      class ReactCallback extends ResultCallback<ConversationResultBase> {

        @Override
        public void onEvent(String eventType, ConversationResultBase message) {
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
      log.info(JSON.toJSONString(ret));
    }
  }

  @Test
  public void testStreamingConversationWithCallback() throws InterruptedException {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .build();

      final List<Object> ret = Lists.newArrayList();
      Semaphore semaphore = new Semaphore(0);
      class ReactCallback extends ResultCallback<ConversationResultBase> {

        @Override
        public void onEvent(String eventType, ConversationResultBase message) {
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
      log.info(JSON.toJSONString(ret));
    }
  }

  @Test
  public void testMultiRoundConversation() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .build();

      Flowable<ConversationResultBase> flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JSON.toJSONString(msg)));
      assert (conversation.getHistory().size() == 2);

      param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1)
              .prompt("May I ask you some questions?").stream(true)
              .build();

      flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JSON.toJSONString(msg)));
      assert (conversation.getHistory().size() == 4);

      param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1)
              .prompt("My name is Siri").stream(true)
              .build();

      flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JSON.toJSONString(msg)));
      assert (conversation.getHistory().size() == 6);

      param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1)
              .prompt("What is my name?").stream(true)
              .build();

      flowable = conversation.streamCall(param);
      List<ConversationResultBase> ret = Lists.newArrayList();
      flowable.blockingForEach(ret::add);
      assert (conversation.getHistory().size() == 8);
      log.info(JSON.toJSONString(ret));
      assert (ret.get(ret.size() - 1).getMessage().getPayload().contains("Siri"));
    }
  }
}
