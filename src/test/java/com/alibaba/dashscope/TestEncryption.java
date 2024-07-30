package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.InvalidateParameter;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.EncryptionConfig;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.alibaba.dashscope.utils.JsonUtils;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKey;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestEncryption {
  private static MockWebServer mockServer;

  @BeforeClass
  public static void before() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s/api/v1/", mockServer.getPort());
    Constants.apiKey = "1234";
  }

  @AfterClass
  public static void after() throws IOException {
    mockServer.close();
  }

  @Test
  public void TestEncryptionRequest()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String publicKeyId = "";
    String publicKey = "";
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);
    SecretKey aesKey = EncryptionUtils.generateAESKey();
    EncryptionConfig encryptionConfig =
        EncryptionConfig.builder()
            .base64PublicKey(publicKey)
            .publicKeyId(publicKeyId)
            .AESEncryptKey(aesKey)
            .iv(iv)
            .build();
    Generation gen = new Generation(Protocol.HTTP.getValue());
    Message userMsg =
        Message.builder().role(Role.USER.getValue()).content("Tell me sometime about AI.").build();
    GenerationParam param =
        GenerationParam.builder()
            .model("qwen-plus")
            .resultFormat("message")
            .messages(Arrays.asList(userMsg))
            .encryptionConfig(encryptionConfig)
            .build();
    String plainOutput =
        "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"Certainly! Artificial Intelligence (AI) is a branch of computer science that aims to create software or machines capable of performing tasks that typically require human intelligence.\"}}]}";
    String responseBody =
        String.format(
            "{\"request_id\":\"1111\",\"usage\":{\"input_tokens\":14,\"output_tokens\":299,\"total_tokens\":313},\"output\":\"%s\"}",
            EncryptionUtils.AESEncrypt(plainOutput, aesKey, iv));
    MockResponse mockResponse = TestUtils.createMockResponse(responseBody, 200);
    mockResponse.addHeader("X-DashScope-OutputEncrypted", "true");
    mockServer.enqueue(mockResponse);

    GenerationResult result = gen.call(param);
    System.out.println(JsonUtils.toJson(result));
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(plainOutput, JsonUtils.toJson(result.getOutput()));
    assertTrue(request.getHeader("X-DashScope-EncryptionKey").contains(publicKeyId));
  }
}
