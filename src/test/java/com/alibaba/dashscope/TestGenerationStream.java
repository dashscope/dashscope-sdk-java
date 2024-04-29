// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestGenerationStream {
  private String msg1 =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"材料\",\"role\":\"assistant\"},\"finish_reason\":\"null\"}]},\"usage\":{\"total_tokens\":27,\"input_tokens\":26,\"output_tokens\":1},\"request_id\":\"975de90c-a678-98d1-a4dd-eb48e7736ec4\"}";
  private String msg2 =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"：\n\",\"role\":\"assistant\"},\"finish_reason\":\"null\"}]},\"usage\":{\"total_tokens\":28,\"input_tokens\":26,\"output_tokens\":2},\"request_id\":\"975de90c-a678-98d1-a4dd-eb48e7736ec4\"}";
  private static MockWebServer mockServer;

  @BeforeAll
  public static void before() throws IOException {
    mockServer = new MockWebServer();
    mockServer.start();
  }

  @AfterAll
  public static void after() throws IOException {
    mockServer.close();
  }

  @Test
  public void testHttpStream()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    MockResponse mockResponse = TestUtils.createStreamMockResponse(Arrays.asList(msg1, msg2), 200);
    mockServer.enqueue(mockResponse);

    int port = mockServer.getPort();
    GenerationParam param =
        GenerationParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("如何做土豆炖猪脚?")
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .topP(0.8)
            .enableSearch(true)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Generation generation = new Generation();
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    List<GenerationResult> results = new ArrayList<>();
    flowable.blockingForEach(
        result -> {
          results.add(result);
        });
    String req = mockServer.takeRequest().getBody().readUtf8();
    System.out.println(req);
    assertTrue(results.size() == 2);
  }
}
