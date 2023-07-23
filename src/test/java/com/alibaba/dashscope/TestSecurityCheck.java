// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.qwen.QWenConversationParam;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
public class TestSecurityCheck {

  String[] protocols = {Protocol.HTTP.getValue(), Protocol.WEBSOCKET.getValue()};

  private static final String DIRTY_WORDS = System.getenv().get("DIRTY_WORDS");

  @BeforeEach
  public void setUp() {
    Constants.init();
  }

  @Test
  public void testSecurityCheck() {
    for (String protocol : protocols) {
      try {
        Conversation conversation = new Conversation(protocol);

        QWenConversationParam param =
            QWenConversationParam.builder()
                .model(QWenConversationParam.QWEN_V1)
                .prompt(DIRTY_WORDS)
                .mode(StreamingMode.OUT)
                .securityCheck(true)
                .apiKey(System.getenv().get("TEST_API_KEY"))
                .maxLength(10)
                .topP(0.8)
                .build();

        Flowable<ConversationResult> flowable = conversation.streamCall(param);
        flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
        assert (false);
      } catch (ApiException e) {
        assert (e.getStatus().getMessage().contains("sensitive content"));
      }
    }
  }

  @Test
  public void testSecurityCheckDefault() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder()
              .model(QWenConversationParam.QWEN_V1)
              .prompt(DIRTY_WORDS)
              .mode(StreamingMode.OUT)
              .apiKey(System.getenv().get("TEST_API_KEY"))
              .maxLength(10)
              .topP(0.8)
              .build();

      Flowable<ConversationResult> flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
    }
  }

  @Test
  public void testSecurityCheckNoStream() {
    for (String protocol : protocols) {
      try {
        Conversation conversation = new Conversation(protocol);

        QWenConversationParam param =
            QWenConversationParam.builder()
                .model(QWenConversationParam.QWEN_V1)
                .prompt(DIRTY_WORDS)
                .securityCheck(true)
                .apiKey(System.getenv().get("TEST_API_KEY"))
                .maxLength(10)
                .topP(0.8)
                .build();

        ConversationResult conversationResult = conversation.call(param);
        log.info(JsonUtils.toJson(conversationResult));
        assert (false);
      } catch (ApiException e) {
        assert (e.getStatus().getMessage().contains("sensitive content"));
      }
    }
  }
}
