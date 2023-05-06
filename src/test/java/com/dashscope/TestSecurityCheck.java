package com.dashscope;

import com.alibaba.fastjson.JSON;
import com.dashscope.common.Protocol;
import com.dashscope.conversation.Conversation;
import com.dashscope.conversation.ConversationResultBase;
import com.dashscope.conversation.qwen.QWenConversationParam;
import com.dashscope.exception.ApiException;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class TestSecurityCheck {

  String[] protocols = {Protocol.HTTP.getValue(), Protocol.WEBSOCKET.getValue()};

  private static final String DIRTY_WORDS = System.getenv().get("DIRTY_WORDS");

  @Test
  public void testSecurityCheck() {
    for (String protocol : protocols) {
      try {
        Conversation conversation = new Conversation(protocol);

        QWenConversationParam param =
            QWenConversationParam.builder().model(QWenConversationParam.QWEN_V1).prompt(DIRTY_WORDS)
                .stream(true)
                .securityCheck(true)
                .maxLength(10)
                .topP(0.8)
                .build();

        Flowable<ConversationResultBase> flowable = conversation.streamCall(param);
        flowable.blockingForEach(msg -> log.info(JSON.toJSONString(msg)));
        assert (false);
      } catch (ApiException e) {
        assert (e.getStatus().getMessage().contains("sensitive content"));
      }
    }
  }
}
