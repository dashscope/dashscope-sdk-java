package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.common.GeneralListParam;
import com.alibaba.dashscope.common.ListResult;
import com.alibaba.dashscope.common.UpdateMetadataParam;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.InvalidateParameter;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.threads.ThreadParam;
import com.alibaba.dashscope.threads.messages.TextMessageParam;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunParam;
import com.alibaba.dashscope.threads.runs.RunStep;
import com.alibaba.dashscope.threads.runs.Runs;
import com.alibaba.dashscope.threads.runs.StepToolCalls;
import com.alibaba.dashscope.threads.runs.SubmitToolOutputsParam;
import com.alibaba.dashscope.threads.runs.ThreadAndRunParam;
import com.alibaba.dashscope.threads.runs.ToolOutput;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolCallFunction.CallFunction;
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
public class TestRuns {
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
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_run_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "value");
    String assistantId = UUID.randomUUID().toString();
    String model = UUID.randomUUID().toString();
    String instructions = UUID.randomUUID().toString();
    String additionalInstructions = UUID.randomUUID().toString();
    List<String> fileIds =
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    float temperature = 100.0f;
    int maxPromptTokens = 100;
    int maxCompletionTokens = 200;
    RunParam.TruncationStrategy truncationStrategy = new RunParam.TruncationStrategy();
    truncationStrategy.setType("last_runs");
    truncationStrategy.setLastMessages(1000);

    TextMessageParam msg1 =
        TextMessageParam.builder()
            .role("user")
            .fileIds(fileIds)
            .metadata(metadata)
            .content("Your are a help assistant.")
            .build();
    TextMessageParam msg2 =
        TextMessageParam.builder().role("user").content(UUID.randomUUID().toString()).build();
    RunParam param =
        RunParam.builder()
            .assistantId(assistantId)
            .model(model)
            .instructions(instructions)
            .additionalInstructions(additionalInstructions)
            .additionalMessage(msg2)
            .additionalMessage(msg1)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .metadata(metadata)
            .temperature(temperature)
            .maxCompletionTokens(maxCompletionTokens)
            .maxPromptTokens(maxPromptTokens)
            .truncationStrategy(truncationStrategy)
            .toolChoice(ToolQuarkSearch.builder().build())
            .build();

    Runs runs = new Runs();
    String threadId = UUID.randomUUID().toString();
    Run run = runs.create(threadId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/runs", threadId);
    TestUtils.verifyRequest(request, param, RunParam.class, expectPath, "POST");
    assertEquals(run.getId(), "run_123");
    assertEquals(run.getThreadId(), "thread_123");
    assertEquals(run.getAssistantId(), "asst_123");
    assertTrue(run.getTools().size() == 3);
    assertEquals(run.getFileIds(), Arrays.asList("file_1", "file_2"));
  }

  @Test
  public void testCreateThreadAndRun()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_run_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "value");
    String assistantId = UUID.randomUUID().toString();
    String model = UUID.randomUUID().toString();
    String instructions = UUID.randomUUID().toString();
    String additionalInstructions = UUID.randomUUID().toString();
    List<String> fileIds =
        Arrays.asList(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString());
    float temperature = 100.0f;
    int maxPromptTokens = 100;
    int maxCompletionTokens = 200;
    RunParam.TruncationStrategy truncationStrategy = new RunParam.TruncationStrategy();
    truncationStrategy.setType("last_runs");
    truncationStrategy.setLastMessages(1000);

    TextMessageParam threadMsg1 =
        TextMessageParam.builder()
            .role("user")
            .content("What is diffusion models?")
            .fileIds(fileIds)
            .metadata(metadata)
            .build();
    TextMessageParam threadMsg2 =
        TextMessageParam.builder()
            .role("assistant")
            .content("Diffusion model is a type of generative model.")
            .build();
    ThreadParam threadParam =
        ThreadParam.builder().message(threadMsg1).message(threadMsg2).metadata(metadata).build();

    TextMessageParam msg1 =
        TextMessageParam.builder()
            .role("user")
            .fileIds(fileIds)
            .metadata(metadata)
            .content("Your are a help assistant.")
            .build();
    TextMessageParam msg2 =
        TextMessageParam.builder().role("user").content(UUID.randomUUID().toString()).build();
    ThreadAndRunParam param =
        ThreadAndRunParam.builder()
            .assistantId(assistantId)
            .thread(threadParam)
            .model(model)
            .instructions(instructions)
            .additionalInstructions(additionalInstructions)
            .additionalMessage(msg2)
            .additionalMessage(msg1)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .metadata(metadata)
            .temperature(temperature)
            .maxCompletionTokens(maxCompletionTokens)
            .maxPromptTokens(maxPromptTokens)
            .truncationStrategy(truncationStrategy)
            .toolChoice(ToolQuarkSearch.builder().build())
            .build();

    Runs runs = new Runs();
    Run run = runs.createThreadAndRun(param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/runs");
    TestUtils.verifyRequest(request, param, RunParam.class, expectPath, "POST");
    assertEquals(run.getId(), "run_123");
    assertEquals(run.getThreadId(), "thread_123");
    assertEquals(run.getAssistantId(), "asst_123");
    assertTrue(run.getTools().size() == 3);
    assertEquals(run.getFileIds(), Arrays.asList("file_1", "file_2"));
  }

  @Test
  public void testList()
      throws IOException, InterruptedException, ApiException, NoApiKeyException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("list_run_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    Runs runs = new Runs();
    String threadId = UUID.randomUUID().toString();
    GeneralListParam param =
        GeneralListParam.builder()
            .limit(1000l)
            .after("after")
            .before("before")
            .order("DESC")
            .build();
    ListResult<Run> runList = runs.list(threadId, param);
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/runs?before=before&limit=1000&after=after&order=DESC", threadId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(runList.getData().size() == 2);
    param = GeneralListParam.builder().limit(1000l).after("after").before("before").build();
    runList = runs.list(threadId, param);
    request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format("/api/v1/threads/%s/runs?before=before&limit=1000&after=after", threadId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(runList.getData().size() == 2);

    param = GeneralListParam.builder().build();
    runList = runs.list(threadId, param);
    request = mockServer.takeRequest();
    assertEquals(request.getPath(), String.format("/api/v1/threads/%s/runs", threadId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(runList.getData().size() == 2);
  }

  @Test
  public void testListSteps()
      throws IOException, InterruptedException, ApiException, NoApiKeyException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("list_run_steps_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    mockServer.enqueue(mockResponse);
    Runs runs = new Runs();
    String threadId = UUID.randomUUID().toString();
    String runId = UUID.randomUUID().toString();
    GeneralListParam param =
        GeneralListParam.builder()
            .limit(1000l)
            .after("after")
            .before("before")
            .order("DESC")
            .build();
    ListResult<RunStep> runStepList = runs.listSteps(threadId, runId, param);
    RecordedRequest request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/runs/%s/steps?before=before&limit=1000&after=after&order=DESC",
            threadId, runId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(runStepList.getData().size() == 2);
    param = GeneralListParam.builder().limit(1000l).after("after").before("before").build();
    runStepList = runs.listSteps(threadId, runId, param);
    request = mockServer.takeRequest();
    assertEquals(
        request.getPath(),
        String.format(
            "/api/v1/threads/%s/runs/%s/steps?before=before&limit=1000&after=after",
            threadId, runId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(runStepList.getData().size() == 2);

    param = GeneralListParam.builder().build();
    runStepList = runs.listSteps(threadId, runId, param);
    request = mockServer.takeRequest();
    assertEquals(
        request.getPath(), String.format("/api/v1/threads/%s/runs/%s/steps", threadId, runId));
    assertEquals(request.getMethod(), "GET");
    assertTrue(runStepList.getData().size() == 2);
  }

  @Test
  public void testRetrieve()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_run_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String threadId = UUID.randomUUID().toString();
    String runId = UUID.randomUUID().toString();
    Runs runs = new Runs();
    Run run = runs.retrieve(threadId, runId);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/runs/%s", threadId, runId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "GET");
    assertEquals(run.getId(), "run_123");
    assertEquals(run.getThreadId(), "thread_123");
    assertEquals(run.getAssistantId(), "asst_123");
    assertTrue(run.getTools().size() == 3);
    assertEquals(run.getFileIds(), Arrays.asList("file_1", "file_2"));
  }

  @Test
  public void testRetrieveStep()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("retrieve_run_step").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String threadId = UUID.randomUUID().toString();
    String runId = UUID.randomUUID().toString();
    String stepId = UUID.randomUUID().toString();
    Runs runs = new Runs();
    RunStep runStep = runs.retrieveStep(threadId, runId, stepId);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath =
        String.format("/api/v1/threads/%s/runs/%s/steps/%s", threadId, runId, stepId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "GET");
    assertEquals(runStep.getId(), "step_1");
    assertEquals(runStep.getRunId(), "run_1");
    assertEquals(runStep.getThreadId(), "thread_1");
    assertEquals(runStep.getAssistantId(), "asst_1");
    StepToolCalls expectToolCalls = new StepToolCalls();
    expectToolCalls.setType("tool_calls");
    ToolCallFunction toolCallFunction = new ToolCallFunction();
    toolCallFunction.setId("call_1");
    toolCallFunction.setType("function");
    CallFunction callFunction = toolCallFunction.new CallFunction();
    callFunction.setName("big_add");
    callFunction.setArguments("{\"left\":87787,\"right\":788988737}");
    callFunction.setOutput("789076524");
    toolCallFunction.setFunction(callFunction);
    expectToolCalls.setToolCalls(Arrays.asList(toolCallFunction));
    assertEquals(runStep.getStepDetails(), expectToolCalls);
  }

  @Test
  public void testUpdate()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("create_run_response").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "value");
    UpdateMetadataParam param = UpdateMetadataParam.builder().metadata(metadata).build();
    String threadId = UUID.randomUUID().toString();
    String runId = UUID.randomUUID().toString();
    Runs runs = new Runs();
    Run run = runs.update(threadId, runId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/runs/%s", threadId, runId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "POST");
    assertEquals(run.getId(), "run_123");
    assertEquals(run.getThreadId(), "thread_123");
    assertEquals(run.getAssistantId(), "asst_123");
    assertTrue(run.getTools().size() == 3);
    assertEquals(run.getFileIds(), Arrays.asList("file_1", "file_2"));
    TestUtils.verifyRequest(request, param, UpdateMetadataParam.class, expectPath, "POST");
  }

  @Test
  public void testSubmitRunOutput()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException {
    String inputFilePath = "./src/test/resources/runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonObject rspObject = jsonObject.get("submit_tool_outputs").getAsJsonObject();
    MockResponse mockResponse = TestUtils.createMockResponse(JsonUtils.toJson(rspObject), 200);
    mockServer.enqueue(mockResponse);
    String callId = UUID.randomUUID().toString();
    String output = UUID.randomUUID().toString();
    SubmitToolOutputsParam param =
        SubmitToolOutputsParam.builder()
            .toolOutput(ToolOutput.builder().toolCallId(callId).output(output).build())
            .build();
    String threadId = UUID.randomUUID().toString();
    String runId = UUID.randomUUID().toString();
    Runs runs = new Runs();
    Run run = runs.submitToolOutputs(threadId, runId, param);
    RecordedRequest request = mockServer.takeRequest();
    String expectPath =
        String.format("/api/v1/threads/%s/runs/%s/submit_tool_outputs", threadId, runId);
    assertEquals(request.getPath(), expectPath);
    assertEquals(request.getMethod(), "POST");
    assertEquals(run.getId(), "run_1");
    assertEquals(run.getThreadId(), "thread_1");
    assertEquals(run.getAssistantId(), "asst_1");
    assertTrue(run.getTools().size() == 1);
    assertTrue(run.getFileIds().size() == 0);
    TestUtils.verifyRequest(request, param, SubmitToolOutputsParam.class, expectPath, "POST");
  }
}
