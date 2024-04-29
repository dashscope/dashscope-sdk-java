package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.dashscope.common.UpdateMetadataParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.threads.AssistantThread;
import com.alibaba.dashscope.threads.ThreadParam;
import com.alibaba.dashscope.threads.Threads;
import com.alibaba.dashscope.threads.messages.TextMessageParam;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestThread {
  private static MockWebServer mockServer;

  public class AddFunctionTool {
    private int left;
    private int right;

    public AddFunctionTool(int left, int right) {
      this.left = left;
      this.right = right;
    }

    public int call() {
      return left + right;
    }
  }

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
  public void testCreate()
      throws IOException, ApiException, NoApiKeyException, InterruptedException {
    String inputFilePath = "./src/test/resources/threads.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("thread_1").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "v");
    List<String> fileIds =
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    TextMessageParam msg1 =
        TextMessageParam.builder()
            .role("user")
            .content("What is diffusion models?")
            .fileIds(fileIds)
            .build();
    TextMessageParam msg2 =
        TextMessageParam.builder()
            .role("assistant")
            .content("Diffusion model is a type of generative model.")
            .build();
    ThreadParam param =
        ThreadParam.builder().message(msg1).message(msg2).metadata(metadata).build();

    Threads threads = new Threads();
    AssistantThread thread = threads.create(param);
    RecordedRequest request = mockServer.takeRequest();
    TestUtils.verifyRequest(request, param, ThreadParam.class, "/api/v1/threads", "POST");
    assertEquals(rspObject.get("id").getAsString(), "thread_1");
    assertEquals(thread.getId(), "thread_1");
    assertEquals(thread.getCreatedAt(), 1709538994);
    Map<String, String> expect = new HashMap<>();
    expect.put("key", "value");
    assertEquals(thread.getMetadata(), expect);
  }

  @Test
  public void testRetrieve()
      throws IOException, ApiException, NoApiKeyException, InterruptedException {
    String inputFilePath = "./src/test/resources/threads.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("thread_1").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String threadId = UUID.randomUUID().toString();
    Threads threads = new Threads();
    AssistantThread thread = threads.retrieve(threadId);
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(request.getPath(), "/api/v1/threads/" + threadId);
    assertEquals(request.getMethod(), "GET");
    assertEquals(rspObject.get("id").getAsString(), "thread_1");
    assertEquals(thread.getId(), "thread_1");
    assertEquals(thread.getCreatedAt(), 1709538994);
    Map<String, String> expect = new HashMap<>();
    expect.put("key", "value");
    assertEquals(thread.getMetadata(), expect);
  }

  @Test
  public void testUpdate()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/threads.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("thread_1").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key1", UUID.randomUUID().toString());
    metadata.put("key2", UUID.randomUUID().toString());
    UpdateMetadataParam param = UpdateMetadataParam.builder().metadata(metadata).build();
    String threadId = UUID.randomUUID().toString();
    Threads threads = new Threads();
    AssistantThread thread = threads.update(threadId, param);
    RecordedRequest request = mockServer.takeRequest();
    TestUtils.verifyRequest(
        request, param, UpdateMetadataParam.class, "/api/v1/threads/" + threadId, "POST");
    assertEquals(rspObject.get("id").getAsString(), "thread_1");
    assertEquals(thread.getId(), "thread_1");
    assertEquals(thread.getCreatedAt(), 1709538994);
    Map<String, String> expect = new HashMap<>();
    expect.put("key", "value");
    assertEquals(thread.getMetadata(), expect);
  }
}
