// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import com.alibaba.dashscope.aigc.multimodalconversation.*;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.ByteString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;
import java.util.Arrays;

import static org.eclipse.jetty.websocket.common.events.annotated.InvalidSignatureException.build;
import static org.junit.Assert.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestMultiModalConversationQwenTTS {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;

  @BeforeEach
  public void before() throws IOException {

    this.server = new MockWebServer();
    this.server.start();

    String responseStr =
            "{\"output\": {\"audio\": {\"data\": \"\", \"expires_at\": 1758187426, \"id\": \"audio_d8ab01f8-2793-4f65-a656-664e6e6c0d19\", \"url\": \"http://dashscope-result.demo.reuslt/abc\"}, \"finish_reason\": \"stop\"}, \"usage\": {\"characters\": 56}, \"request_id\": \"d8ab01f8-2793-4f65-a656-664e6e6c0d19\"}";
    server.enqueue(
            new MockResponse()
                    .setBody(responseStr)
                    .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void testSendAndReceive()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException, UploadFileException {
    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    MultiModalConversation conv = new MultiModalConversation();

    MultiModalConversationParam param =
        MultiModalConversationParam.builder()
              .model("qwen-tts-lastest")
              .text("Today is a wonderful day to build something people love!")
              .voice(AudioParameters.Voice.DYLAN)
              .languageType("zh")
             .build();
    MultiModalConversationResult result = conv.call(param);
    RecordedRequest request = this.server.takeRequest();
    System.out.println("request:" + request.getBody().readUtf8());
    System.out.println(JsonUtils.toJson(result));

  }
}
