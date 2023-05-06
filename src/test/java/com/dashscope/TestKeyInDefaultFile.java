package com.dashscope;

import com.dashscope.common.Protocol;
import com.dashscope.conversation.Conversation;
import com.dashscope.conversation.ConversationResult;
import com.dashscope.conversation.qwen.QWenConversationParam;
import com.dashscope.mock.MockServer;
import com.dashscope.utils.Constants;
import com.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
public class TestKeyInDefaultFile {

  String[] protocols = {Protocol.HTTP.getValue(), Protocol.WEBSOCKET.getValue()};

  private final MockServer mockServer = new MockServer();

  private Path dashscopePath;

  private String oldBaseHttpApiUrl = null;

  private String oldBaseWebsocketApiUrl = null;

  @BeforeEach
  public void setUp() throws IOException {
    oldBaseHttpApiUrl = Constants.baseHttpApiUrl;
    oldBaseWebsocketApiUrl = Constants.baseWebsocketApiUrl;
    Constants.baseHttpApiUrl = "http://localhost:8080/api/v1";
    Constants.baseWebsocketApiUrl = "ws://localhost:8080/api-ws/v1/inference";
    mockServer.start(0, "testKey");

    Path homePath = Paths.get(System.getProperty("user.home"));
    dashscopePath = homePath.resolve(".dashscope");
    Files.createDirectories(dashscopePath);
    dashscopePath = dashscopePath.resolve("api_key");
    Files.write(dashscopePath, "testKey".getBytes());
    Constants.init();
  }

  @AfterEach
  public void tearDown() throws IOException {
    Constants.baseHttpApiUrl = oldBaseHttpApiUrl;
    Constants.baseWebsocketApiUrl = oldBaseWebsocketApiUrl;
    mockServer.tearDown();
    Files.delete(dashscopePath);
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
              .build();

      Flowable<ConversationResult> flowable = conversation.streamCall(param);
      flowable.blockingForEach(msg -> log.info(JsonUtils.toJson(msg)));
      assert (conversation.getHistory().size() == 2);
    }
  }
}
