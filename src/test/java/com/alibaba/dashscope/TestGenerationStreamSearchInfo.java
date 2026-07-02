// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.*;

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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestGenerationStreamSearchInfo {
  private static MockWebServer mockServer;

  // Choices format messages - searchInfo appears at output level in first chunk
  private String choicesMsg1WithSearchInfo =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"今天\",\"role\":\"assistant\"},\"finish_reason\":\"null\"}],\"search_info\":{\"search_results\":[{\"site_name\":\"weather.com\",\"icon\":\"\",\"index\":1,\"title\":\"Weather\",\"url\":\"https://weather.com\"}]}},\"usage\":{\"total_tokens\":27,\"input_tokens\":26,\"output_tokens\":1},\"request_id\":\"test-req-1\"}";
  private String choicesMsg2NoSearchInfo =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"晴天\",\"role\":\"assistant\"},\"finish_reason\":\"stop\"}]},\"usage\":{\"total_tokens\":28,\"input_tokens\":26,\"output_tokens\":2},\"request_id\":\"test-req-1\"}";

  // Choices format messages - searchInfo appears at output level in middle chunk
  private String choicesMsg3NoSearchInfo =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"明天\",\"role\":\"assistant\"},\"finish_reason\":\"null\"}]},\"usage\":{\"total_tokens\":27,\"input_tokens\":26,\"output_tokens\":1},\"request_id\":\"test-req-2\"}";
  private String choicesMsg4WithSearchInfo =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"多云\",\"role\":\"assistant\"},\"finish_reason\":\"null\"}],\"search_info\":{\"search_results\":[{\"site_name\":\"weather2.com\",\"icon\":\"\",\"index\":1,\"title\":\"Weather2\",\"url\":\"https://weather2.com\"}]}},\"usage\":{\"total_tokens\":28,\"input_tokens\":26,\"output_tokens\":2},\"request_id\":\"test-req-2\"}";
  private String choicesMsg5NoSearchInfo =
      "{\"output\":{\"choices\":[{\"message\":{\"content\":\"转晴\",\"role\":\"assistant\"},\"finish_reason\":\"stop\"}]},\"usage\":{\"total_tokens\":29,\"input_tokens\":26,\"output_tokens\":3},\"request_id\":\"test-req-2\"}";

  // Legacy text format messages - searchInfo appears in first chunk
  private String legacyMsg1WithSearchInfo =
      "{\"output\":{\"text\":\"今天\",\"search_info\":{\"search_results\":[{\"site_name\":\"weather.com\",\"icon\":\"\",\"index\":1,\"title\":\"Weather\",\"url\":\"https://weather.com\"}]}},\"usage\":{\"total_tokens\":27,\"input_tokens\":26,\"output_tokens\":1},\"request_id\":\"test-req-3\"}";
  private String legacyMsg2NoSearchInfo =
      "{\"output\":{\"text\":\"晴天\"},\"usage\":{\"total_tokens\":28,\"input_tokens\":26,\"output_tokens\":2},\"request_id\":\"test-req-3\"}";

  // Legacy text format messages - searchInfo appears in second chunk
  private String legacyMsg3NoSearchInfo =
      "{\"output\":{\"text\":\"明天\"},\"usage\":{\"total_tokens\":27,\"input_tokens\":26,\"output_tokens\":1},\"request_id\":\"test-req-4\"}";
  private String legacyMsg4WithSearchInfo =
      "{\"output\":{\"text\":\"多云\",\"search_info\":{\"search_results\":[{\"site_name\":\"weather.com\",\"icon\":\"\",\"index\":1,\"title\":\"Weather\",\"url\":\"https://weather.com\"}]}},\"usage\":{\"total_tokens\":28,\"input_tokens\":26,\"output_tokens\":2},\"request_id\":\"test-req-4\"}";

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
  public void testSearchInfoPreservedInChoicesFormat_FirstChunk()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    // SearchInfo appears in first chunk, should be preserved in all subsequent chunks
    MockResponse mockResponse =
        TestUtils.createStreamMockResponse(
            Arrays.asList(choicesMsg1WithSearchInfo, choicesMsg2NoSearchInfo), 200);
    mockServer.enqueue(mockResponse);

    GenerationParam param =
        GenerationParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("test")
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .incrementalOutput(false)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", mockServer.getPort());

    Generation generation = new Generation();
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    List<GenerationResult> results = new ArrayList<>();
    flowable.blockingForEach(results::add);

    assertEquals(2, results.size());

    // Debug: print results
    System.out.println("=== Choices First Chunk Test ===");
    for (int i = 0; i < results.size(); i++) {
      System.out.println(
          "Result " + i + ": " + com.alibaba.dashscope.utils.JsonUtils.toJson(results.get(i)));
      System.out.println("  searchInfo: " + results.get(i).getOutput().getSearchInfo());
    }

    // First chunk should have searchInfo at output level
    assertNotNull(results.get(0).getOutput().getChoices());
    assertNotNull(results.get(0).getOutput().getSearchInfo());
    assertEquals(
        "weather.com",
        results.get(0).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());

    // Second chunk should also have searchInfo at output level (accumulated from first chunk)
    assertNotNull(results.get(1).getOutput().getChoices());
    assertNotNull(results.get(1).getOutput().getSearchInfo());
    assertEquals(
        "weather.com",
        results.get(1).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());
  }

  @Test
  public void testSearchInfoPreservedInChoicesFormat_MiddleChunk()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    // SearchInfo appears in middle chunk, should be preserved in all chunks including later ones
    MockResponse mockResponse =
        TestUtils.createStreamMockResponse(
            Arrays.asList(
                choicesMsg3NoSearchInfo, choicesMsg4WithSearchInfo, choicesMsg5NoSearchInfo),
            200);
    mockServer.enqueue(mockResponse);

    GenerationParam param =
        GenerationParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("test")
            .resultFormat(GenerationParam.ResultFormat.MESSAGE)
            .incrementalOutput(false)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", mockServer.getPort());

    Generation generation = new Generation();
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    List<GenerationResult> results = new ArrayList<>();
    flowable.blockingForEach(results::add);

    assertEquals(3, results.size());

    // First chunk has no searchInfo at output level
    assertNull(results.get(0).getOutput().getSearchInfo());

    // Second chunk has searchInfo at output level
    assertNotNull(results.get(1).getOutput().getSearchInfo());
    assertEquals(
        "weather2.com",
        results.get(1).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());

    // Third chunk should also have searchInfo at output level (accumulated from second chunk)
    assertNotNull(results.get(2).getOutput().getSearchInfo());
    assertEquals(
        "weather2.com",
        results.get(2).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());
  }

  @Test
  public void testSearchInfoPreservedInLegacyFormat_FirstChunk()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    // SearchInfo appears in first chunk of legacy format, should be preserved in second chunk
    MockResponse mockResponse =
        TestUtils.createStreamMockResponse(
            Arrays.asList(legacyMsg1WithSearchInfo, legacyMsg2NoSearchInfo), 200);
    mockServer.enqueue(mockResponse);

    GenerationParam param =
        GenerationParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("test")
            .resultFormat(GenerationParam.ResultFormat.TEXT)
            .incrementalOutput(false)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", mockServer.getPort());

    Generation generation = new Generation();
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    List<GenerationResult> results = new ArrayList<>();
    flowable.blockingForEach(results::add);

    assertEquals(2, results.size());

    // First chunk should have searchInfo at output level
    assertNotNull(results.get(0).getOutput().getSearchInfo());
    assertEquals(
        "weather.com",
        results.get(0).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());

    // Second chunk should also have searchInfo at output level (accumulated from first chunk)
    assertNotNull(results.get(1).getOutput().getSearchInfo());
    assertEquals(
        "weather.com",
        results.get(1).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());
  }

  @Test
  public void testSearchInfoPreservedInLegacyFormat_SecondChunk()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    // SearchInfo appears in second chunk of legacy format
    MockResponse mockResponse =
        TestUtils.createStreamMockResponse(
            Arrays.asList(legacyMsg3NoSearchInfo, legacyMsg4WithSearchInfo), 200);
    mockServer.enqueue(mockResponse);

    GenerationParam param =
        GenerationParam.builder()
            .model(Generation.Models.QWEN_TURBO)
            .prompt("test")
            .resultFormat(GenerationParam.ResultFormat.TEXT)
            .incrementalOutput(false)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", mockServer.getPort());

    Generation generation = new Generation();
    Flowable<GenerationResult> flowable = generation.streamCall(param);
    List<GenerationResult> results = new ArrayList<>();
    flowable.blockingForEach(results::add);

    assertEquals(2, results.size());

    // First chunk has no searchInfo at output level
    assertNull(results.get(0).getOutput().getSearchInfo());

    // Second chunk has searchInfo at output level
    assertNotNull(results.get(1).getOutput().getSearchInfo());
    assertEquals(
        "weather.com",
        results.get(1).getOutput().getSearchInfo().getSearchResults().get(0).getSiteName());
  }
}
