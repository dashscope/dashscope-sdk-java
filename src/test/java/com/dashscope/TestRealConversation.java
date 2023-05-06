package com.dashscope;

import com.dashscope.common.Protocol;
import com.dashscope.conversation.Conversation;
import com.dashscope.conversation.ConversationResult;
import com.dashscope.conversation.qwen.QWenConversationParam;
import com.dashscope.utils.Constants;
import com.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
public class TestRealConversation {

  String[] protocols = {Protocol.HTTP.getValue(), Protocol.WEBSOCKET.getValue()};

  @BeforeEach
  public void setUp() {
    Constants.init();
  }

  @Test
  public void testBasicConversation() {
    for (String protocol : protocols) {
      Conversation conversation = new Conversation(protocol);

      QWenConversationParam param =
          QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt("hello")
              .stream(true)
              .maxLength(10)
              .topP(0.8)
              .apiKey(System.getenv().get("TEST_API_KEY"))
              .build();

      Flowable<ConversationResult> flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
      assert (conversation.getHistory().size() == 2);
    }
  }
}
