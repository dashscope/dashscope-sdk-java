package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.InvalidateParameter;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
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
    Generation gen = new Generation(Protocol.HTTP.getValue());
    Message userMsg =
        Message.builder().role(Role.USER.getValue()).content("Tell me sometime about AI.").build();
    GenerationParam param =
        GenerationParam.builder()
            .model("qwen-plus")
            .resultFormat("message")
            .messages(Arrays.asList(userMsg))
            .enableEncrypt(true)
            .build();
    String plainOutput =
        "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"role\":\"assistant\",\"content\":\"Certainly! Artificial Intelligence (AI) is a branch of computer science that aims to create software or machines capable of performing tasks that typically require human intelligence.\"}}]}";
    String publicKeyId = "1111111111111";
    String publicKey =
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnojrB579xgPQN5f46SvoRAiQBPWBaPzWh7hp51fWI+OsQk7KqH0qMcw8i0eK5rfOvJIPujOQgnes1ph9/gKAst9NzXVIl9JJYUSPtzTvOabhp4yvS3KBf9g3xHYVjYgW33SOY74Ue/tgbCXn717rV6gXb4sVvq9XK/1BrDcGbEOQEZEgBTFkm/g3lpWLQtACwwqHffoA9eQtkkz15ZFKosAgbR8LedfIvxAl2zk15REzxXiRcFgc9/tLF0U1t2Sxt9FkQefxYwn6EZawTsRJvf4kqF3MaPdTcDbOp0iSNvCl2qzPSf/F+Oll2CUM1tFAEu81oaaaaWaDR3UtvqOtyQIDAQAB";
    String keyBody =
        String.format(
            "{\"data\":{\"public_key_id\":\"%s\",\"public_key\":\"%s\"},\"request_id\":\"5e5da6b\"}",
            publicKeyId, publicKey);
    MockResponse getKeyResponse = TestUtils.createMockResponse(keyBody, 200);
    mockServer.enqueue(getKeyResponse);
    Semaphore semaphore = new Semaphore(0);
    List<GenerationResult> results = new ArrayList<>();
    gen.call(
        param,
        new ResultCallback<GenerationResult>() {

          @Override
          public void onEvent(GenerationResult message) {
            results.add(message);
          }

          @Override
          public void onError(Exception ex) {
            System.out.println(ex.getMessage());
            semaphore.release();
          }

          @Override
          public void onComplete() {
            System.out.println("onComplete");
            semaphore.release();
          }
        });
    RecordedRequest request = mockServer.takeRequest(); // get the request
    assertEquals(request.getPath(), "/api/v1/public-keys/latest");
    RecordedRequest messageRequest = mockServer.takeRequest();
    String encryptString = messageRequest.getHeader("X-DashScope-EncryptionKey");
    JsonObject encryptInfo = JsonUtils.parse(encryptString);
    String encryptKey = encryptInfo.get("encrypt_key").getAsString();
    String ivs = encryptInfo.get("iv").getAsString();
    assertNotNull(encryptKey);
    assertNotNull(ivs);
  }
}
