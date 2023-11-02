package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.dashscope.aigc.generation.models.QwenParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tokenizers.Tokenization;
import com.alibaba.dashscope.tokenizers.TokenizationOutput;
import com.alibaba.dashscope.tokenizers.TokenizationResult;
import com.alibaba.dashscope.tokenizers.TokenizationUsage;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import java.io.IOException;
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
public class TestTokenizor {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;
  private TestResponse rsp;
  private TokenizationOutput output;
  private TokenizationUsage usage;
  private String requestId;
  private String expectBody =
      "{\"model\":\"qwen-turbo\",\"input\":{\"prompt\":\"如何做土豆炖猪脚?\"},\"parameters\":{\"top_p\":0.8,\"seed\":100}}";

  @BeforeEach
  public void before() {
    String outputStr =
        "{\"token_ids\":[100007,99190,109971,110798,100761,100037,30],\"tokens\":[\"如何\",\"做\",\"土豆\",\"炖\",\"猪\",\"脚\",\"?\"]}";
    output = JsonUtils.fromJson(outputStr, TokenizationOutput.class);
    usage = new TokenizationUsage();
    usage.setInputTokens(7);
    requestId = "682d7353-5100-9054-b44b-76f0cb045b37";
    rsp =
        TestResponse.builder()
            .output(JsonUtils.toJsonObject(output))
            .usage(JsonUtils.toJsonObject(usage))
            .requestId(requestId)
            .build();
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  private void checkResult(TokenizationResult result, RecordedRequest request) {
    assertEquals(result.getRequestId(), requestId);
    assertEquals(result.getOutput(), output);
    assertEquals(result.getUsage(), usage);
    String body = request.getBody().readUtf8();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/tokenizer");
    assertEquals(expectBody, body);
  }

  @Test
  public void testTokenization()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    Tokenization tokenizer = new Tokenization();
    QwenParam param =
        QwenParam.builder()
            .model(Tokenization.Models.QWEN_TURBO)
            .prompt("如何做土豆炖猪脚?")
            .topP(0.8)
            .seed(100)
            .build();

    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    TokenizationResult result = tokenizer.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request);
  }
}
