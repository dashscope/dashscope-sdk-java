package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.assistants.Assistant;
import com.alibaba.dashscope.assistants.AssistantFile;
import com.alibaba.dashscope.assistants.AssistantFileParam;
import com.alibaba.dashscope.assistants.AssistantParam;
import com.alibaba.dashscope.assistants.Assistants;
import com.alibaba.dashscope.common.DeletionStatus;
import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.tools.search.ToolQuarkSearch;
import com.alibaba.dashscope.tools.wanx.ToolWanX;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
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
public class TestAssistants {
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

  static ToolFunction buildFunction() {
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    SchemaGeneratorConfig config =
        configBuilder
            .with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
            .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
            .build();
    SchemaGenerator generator = new SchemaGenerator(config);

    // generate jsonSchema of function.
    ObjectNode jsonSchema = generator.generateSchema(AddFunctionTool.class);

    // call with tools of function call, jsonSchema.toString() is jsonschema String.
    FunctionDefinition fd =
        FunctionDefinition.builder()
            .name("add")
            .description("add two number")
            .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject())
            .build();
    return ToolFunction.builder().function(fd).build();
  }

  @Test
  public void testCreate()
      throws IOException, ApiException, NoApiKeyException, InterruptedException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("test_function_call_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String model = UUID.randomUUID().toString();
    String name = UUID.randomUUID().toString();
    String description = UUID.randomUUID().toString();
    String instructions = UUID.randomUUID().toString();
    ToolFunction toolFunction = buildFunction();
    Map<String, String> metadata = new HashMap<>();
    List<String> fileIds =
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    AssistantParam param =
        AssistantParam.builder()
            .model(model)
            .name(name)
            .description(description)
            .instructions(instructions)
            .metadata(metadata)
            .fileIds(fileIds)
            .tools(
                Arrays.asList(
                    toolFunction, ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .parameter("str", "hello")
            .parameter("obj", toolFunction)
            .parameter("float", 1.0f)
            .parameter("double", 10000.0)
            .parameter("null", null)
            .parameter("array", fileIds)
            .build();

    Assistants assistants = new Assistants();
    Assistant assistant = assistants.create(param);
    RecordedRequest request = mockServer.takeRequest();
    TestUtils.verifyRequest(request, param, AssistantParam.class, "/api/v1/assistants", "POST");
    assertEquals(rspObject.get("model").getAsString(), assistant.getModel());
  }

  @Test
  public void testList() throws IOException, InterruptedException, ApiException, NoApiKeyException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("test_list_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    Assistants assistants = new Assistants();
    GeneralListParam param =
        GeneralListParam.builder()
            .limit(1000l)
            .after("after")
            .before("before")
            .order("DESC")
            .build();
    ListResult<Assistant> assistantList = assistants.list(param);
    RecordedRequest request = mockServer.takeRequest();
    String path = request.getPath();
    System.out.println(path);
    assertEquals(
        request.getPath(), "/api/v1/assistants?before=before&limit=1000&after=after&order=DESC");
    assertEquals(request.getMethod(), "GET");
    assertTrue(assistantList.getData().size() == 2);
    param = GeneralListParam.builder().limit(1000l).after("after").before("before").build();
    assistantList = assistants.list(param);
    request = mockServer.takeRequest();
    assertEquals(request.getPath(), "/api/v1/assistants?before=before&limit=1000&after=after");
    assertEquals(request.getMethod(), "GET");
    assertTrue(assistantList.getData().size() == 2);

    param = GeneralListParam.builder().build();
    assistantList = assistants.list(param);
    request = mockServer.takeRequest();
    assertEquals(request.getPath(), "/api/v1/assistants");
    assertEquals(request.getMethod(), "GET");
    assertTrue(assistantList.getData().size() == 2);
  }

  @Test
  public void testRetrieve()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("test_function_call_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String assistantId = UUID.randomUUID().toString();
    Assistants assistants = new Assistants();
    Assistant assistant = assistants.retrieve(assistantId);
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(request.getPath(), "/api/v1/assistants/" + assistantId);
    assertEquals(request.getMethod(), "GET");
    assertEquals(rspObject.get("model").getAsString(), assistant.getModel());
  }

  @Test
  public void testUpdate()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("test_function_call_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String model = UUID.randomUUID().toString();
    String name = UUID.randomUUID().toString();
    String description = UUID.randomUUID().toString();
    String instructions = UUID.randomUUID().toString();
    Map<String, String> metadata = new HashMap<>();
    List<String> fileIds =
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    AssistantParam param =
        AssistantParam.builder()
            .model(model)
            .name(name)
            .description(description)
            .instructions(instructions)
            .metadata(metadata)
            .fileIds(fileIds)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .build();
    String assistantId = UUID.randomUUID().toString();
    Assistants assistants = new Assistants();
    Assistant assistant = assistants.update(assistantId, param);
    RecordedRequest request = mockServer.takeRequest();
    TestUtils.verifyRequest(
        request, param, AssistantParam.class, "/api/v1/assistants/" + assistantId, "POST");
    assertEquals(rspObject.get("model").getAsString(), assistant.getModel());
  }

  @Test
  public void testDelete()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String id = UUID.randomUUID().toString();
    String deleteStatusString =
        String.format("{\"id\": \"%s\",\"deleted\": true,\"object\": \"assistant\"}", id);
    MockResponse mockResponse = TestUtils.createMockResponse(deleteStatusString, 200);
    mockServer.enqueue(mockResponse);
    Assistants assistants = new Assistants();
    DeletionStatus deletionStatus = assistants.delete(id);
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(request.getPath(), "/api/v1/assistants/" + id);
    assertEquals(request.getMethod(), "DELETE");
    assertEquals(deletionStatus.getId(), id);
    assertEquals(deletionStatus.getObject(), "assistant");
  }

  @Test
  public void testCreateFile()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_assistant_file").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String fileId = UUID.randomUUID().toString();
    AssistantFileParam param = AssistantFileParam.builder().fileId(fileId).build();

    String assistantId = UUID.randomUUID().toString();

    Assistants assistants = new Assistants();
    AssistantFile assistantFile = assistants.createFile(assistantId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/assistants/%s/files", assistantId);
    TestUtils.verifyRequest(request, param, AssistantFileParam.class, expectPath, "POST");
    assertEquals(assistantFile.getAssistantId(), "asst_1");
    assertEquals(assistantFile.getId(), "file_1");
  }

  @Test
  public void testRetrieveFile()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_assistant_file").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String fileId = UUID.randomUUID().toString();

    String assistantId = UUID.randomUUID().toString();

    Assistants assistants = new Assistants();
    AssistantFile assistantFile = assistants.retrieveFile(assistantId, fileId, null);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/assistants/%s/files/%s", assistantId, fileId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(assistantFile.getAssistantId(), "asst_1");
    assertEquals(assistantFile.getId(), "file_1");
  }

  @Test
  public void testListFile()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/assistant.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("list_assistant_files").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);

    String assistantId = UUID.randomUUID().toString();

    Assistants assistants = new Assistants();
    GeneralListParam param =
        GeneralListParam.builder()
            .limit(1000l)
            .after("after")
            .before("before")
            .order("DESC")
            .build();
    ListResult<AssistantFile> assistantFileList = assistants.listFiles(assistantId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath =
        String.format(
            "/api/v1/assistants/%s/files?before=before&limit=1000&after=after&order=DESC",
            assistantId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "GET");
    assertTrue(assistantFileList.getData().size() == 2);
    assertEquals(assistantFileList.getData().get(0).getId(), "file_1");
  }
}
