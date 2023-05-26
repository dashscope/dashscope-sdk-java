// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.qwen.QWenConversationParam;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.StreamingMode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.mock.MockServer;
import com.alibaba.dashscope.utils.Constants;
import com.google.gson.Gson;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
public class TestConversationError {

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
    mockServer.start(1000, "testKey");
  }

  @AfterEach
  public void tearDown() {
    Constants.baseHttpApiUrl = oldBaseHttpApiUrl;
    Constants.baseWebsocketApiUrl = oldBaseWebsocketApiUrl;
    mockServer.tearDown();
  }

  @Test
  public void testBasicConversationError() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder()
              .model(QWenConversationParam.QWEN_V1)
              .prompt("hello")
              .mode(StreamingMode.OUT)
              .maxLength(10)
              .topP(0.8)
              .apiKey("testKey")
              .build();

      Flowable<ConversationResult> flowable = conversation.streamCall(param);
      try {
        flowable.blockingForEach(msg -> log.info(new Gson().toJson(msg)));
        assert (false);
      } catch (Throwable e) {
        log.error("Error happens", e);
        assert (e instanceof ApiException);
        assert (((ApiException) e).getStatus().getCode().equals("1000"));
      }
    }
  }
}
