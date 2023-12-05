// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationMessage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemImage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemText;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestMultiModalConversation {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;

  @BeforeEach
  public void before() {
    this.server = new MockWebServer();
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
    String responseStr =
        "{\"request_id\": \"05753423-9cb2-9347-bf34-9c718bf45597\", \"output\": {\"text\": null, \"finish_reason\": null, \"choices\": [{\"finish_reason\": \"stop\", \"message\": {\"role\": \"assistant\", \"content\": \"\\u6839\\u636e\\u6211\\u6240\\u5728\\u7684\\u4f4d\\u7f6e\\uff0c\\u4eca\\u5929\\u7684\\u5929\\u6c14\\u60c5\\u51b5\\u662f\\u6674\\u6717\\u7684\\uff0c\\u6c14\\u6e29\\u5927\\u7ea6\\u572825-30\\u6444\\u6c0f\\u5ea6\\u4e4b\\u95f4\\uff0c\\u98ce\\u529b\\u8f83\\u5927\\uff0c\\u591a\\u4e91\\u6709\\u96e8\\u3002\"}}]}, \"usage\": {\"input_tokens\": 26, \"output_tokens\": 46}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseStr)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    MultiModalConversation conv = new MultiModalConversation();
    MultiModalMessageItemText systemText =
        new MultiModalMessageItemText("You are a helpful assistant.");
    MultiModalConversationMessage systemMessage =
        MultiModalConversationMessage.builder()
            .role(Role.SYSTEM.getValue())
            .content(Arrays.asList(systemText))
            .build();
    MultiModalMessageItemImage userImage =
        new MultiModalMessageItemImage(
            "https://data-generator-idst.oss-cn-shanghai.aliyuncs.com/dashscope/image/multi_embedding/image/256_1.png");
    MultiModalMessageItemText userText = new MultiModalMessageItemText("图片里有动物吗?");
    MultiModalConversationMessage userMessage =
        MultiModalConversationMessage.builder()
            .role(Role.USER.getValue())
            .content(Arrays.asList(userImage, userText))
            .build();
    MultiModalConversationParam param =
        MultiModalConversationParam.builder()
            .model("chat-dev1")
            .message(systemMessage)
            .message(userMessage)
            .build();
    MultiModalConversationResult result = conv.call(param);
    RecordedRequest request = server.takeRequest();
    assertEquals(result.getRequestId(), "05753423-9cb2-9347-bf34-9c718bf45597");
    MultiModalMessage msg = result.getOutput().getChoices().get(0).getMessage();
    assertEquals(
        msg.getContent().get(0).get("text"),
        "\u6839\u636e\u6211\u6240\u5728\u7684\u4f4d\u7f6e\uff0c\u4eca\u5929\u7684\u5929\u6c14\u60c5\u51b5\u662f\u6674\u6717\u7684\uff0c\u6c14\u6e29\u5927\u7ea6\u572825-30\u6444\u6c0f\u5ea6\u4e4b\u95f4\uff0c\u98ce\u529b\u8f83\u5927\uff0c\u591a\u4e91\u6709\u96e8\u3002");
    assertEquals(result.getUsage().getInputTokens(), (Integer) 26);
    assertEquals(result.getUsage().getOutputTokens(), (Integer) 46);
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/aigc/multimodal-generation/generation");
  }
}
