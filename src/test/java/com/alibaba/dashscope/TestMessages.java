package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.common.UpdateMetadataParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.threads.ContentText;
import com.alibaba.dashscope.threads.messages.MessageFile;
import com.alibaba.dashscope.threads.messages.Messages;
import com.alibaba.dashscope.threads.messages.TextMessageParam;
import com.alibaba.dashscope.threads.messages.ThreadMessage;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
public class TestMessages {
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
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/messages.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_message_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "value");
    List<String> fileIds =
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    TextMessageParam param =
        TextMessageParam.builder()
            .role("user")
            .fileIds(fileIds)
            .metadata(metadata)
            .content("Your are a help assistant.")
            .build();

    Messages messages = new Messages();
    String threadId = UUID.randomUUID().toString();
    ThreadMessage message = messages.create(threadId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/messages", threadId);
    TestUtils.verifyRequest(request, param, TextMessageParam.class, expectPath, "POST");
    assertEquals(message.getId(), "msg_created");
    assertEquals(message.getThreadId(), "thread_create");
    ContentText text = (ContentText) message.getContent().get(0);
    assertEquals(text.getType(), "text");
    assertEquals(text.getText().getAnnotations(), new ArrayList<>());
    assertEquals(text.getText().getValue(), "Who are you.");
  }

  @Test
  public void testList()
      throws IOException, InterruptedException, ApiException, NoApiKeyException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/messages.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("list_message_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    Messages messages = new Messages();
    String threadId = UUID.randomUUID().toString();
    GeneralListParam param =
        GeneralListParam.builder()
            .limit(1000l)
            .after("after")
            .before("before")
            .order("DESC")
            .build();
    ListResult<ThreadMessage> threadList = messages.list(threadId, param);
    RecordedRequest request = mockServer.takeRequest();
    String path = request.getPath();
    System.out.println(path);
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/messages?before=before&limit=1000&after=after&order=DESC",
            threadId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(threadList.getData().size() == 2);
    ContentText text = (ContentText) threadList.getData().get(1).getContent().get(0);
    assertEquals(text.getText().getValue(), "Who are you.");
    param = GeneralListParam.builder().limit(1000l).after("after").before("before").build();
    threadList = messages.list(threadId, param);
    request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/messages?before=before&limit=1000&after=after", threadId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(threadList.getData().size() == 2);

    param = GeneralListParam.builder().build();
    threadList = messages.list(threadId, param);
    request = mockServer.takeRequest();
    assertEquals(request.getPath(), String.format("/api/v1/threads/%s/messages", threadId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(threadList.getData().size() == 2);
  }

  @Test
  public void testRetrieve()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/messages.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_message_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String threadId = UUID.randomUUID().toString();
    String messageId = UUID.randomUUID().toString();
    Messages messages = new Messages();
    ThreadMessage threadMessage = messages.retrieve(threadId, messageId);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/messages/%s", threadId, messageId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "GET");
    assertEquals(threadMessage.getId(), "msg_created");
    assertEquals(threadMessage.getThreadId(), "thread_create");
    ContentText text = (ContentText) threadMessage.getContent().get(0);
    assertEquals(text.getType(), "text");
    assertEquals(text.getText().getAnnotations(), new ArrayList<>());
    assertEquals(text.getText().getValue(), "Who are you.");
  }

  @Test
  public void testUpdate()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/messages.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_message_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "value");
    UpdateMetadataParam param = UpdateMetadataParam.builder().metadata(metadata).build();
    String threadId = UUID.randomUUID().toString();
    String messageId = UUID.randomUUID().toString();
    Messages messages = new Messages();
    ThreadMessage threadMessage = messages.update(threadId, messageId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/messages/%s", threadId, messageId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "POST");
    assertEquals(threadMessage.getId(), "msg_created");
    assertEquals(threadMessage.getThreadId(), "thread_create");
    ContentText text = (ContentText) threadMessage.getContent().get(0);
    assertEquals(text.getType(), "text");
    assertEquals(text.getText().getAnnotations(), new ArrayList<>());
    assertEquals(text.getText().getValue(), "Who are you.");

    TestUtils.verifyRequest(request, param, UpdateMetadataParam.class, expectPath, "POST");
  }

  @Test
  public void testListFiles()
      throws IOException, InterruptedException, ApiException, NoApiKeyException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/messages.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("list_message_files_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    Messages messages = new Messages();
    String threadId = UUID.randomUUID().toString();
    String messageId = UUID.randomUUID().toString();
    GeneralListParam param =
        GeneralListParam.builder()
            .limit(1000l)
            .after("after")
            .before("before")
            .order("DESC")
            .build();
    ListResult<MessageFile> messageFilesList = messages.listFiles(threadId, messageId, param);
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/messages/%s/files?before=before&limit=1000&after=after&order=DESC",
            threadId, messageId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(messageFilesList.getData().size() == 2);
    assertEquals(messageFilesList.getData().get(0).getId(), "file-1");
    param = GeneralListParam.builder().limit(1000l).after("after").before("before").build();
    messageFilesList = messages.listFiles(threadId, messageId, param);
    request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/messages/%s/files?before=before&limit=1000&after=after",
            threadId, messageId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(messageFilesList.getData().size() == 2);

    param = GeneralListParam.builder().build();
    messageFilesList = messages.listFiles(threadId, messageId, param);
    request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format("/api/v1/threads/%s/messages/%s/files", threadId, messageId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(messageFilesList.getData().size() == 2);
  }

  @Test
  public void testRetrieveFile()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/messages.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("retrieve_message_file_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String threadId = UUID.randomUUID().toString();
    String messageId = UUID.randomUUID().toString();
    String fileId = UUID.randomUUID().toString();
    Messages messages = new Messages();
    MessageFile messageFile = messages.retrieveFile(threadId, messageId, fileId);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath =
        String.format("/api/v1/threads/%s/messages/%s/files/%s", threadId, messageId, fileId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "GET");
    assertEquals(messageFile.getId(), "file-2");
    assertEquals(messageFile.getMessageId(), "msg_1");
  }
}
