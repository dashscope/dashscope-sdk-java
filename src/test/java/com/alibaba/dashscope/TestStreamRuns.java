package com.alibaba.dashscope;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.InvalidateParameter;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.threads.AssistantStreamEvents;
import com.alibaba.dashscope.threads.ThreadParam;
import com.alibaba.dashscope.threads.messages.TextMessageParam;
import com.alibaba.dashscope.threads.runs.AssistantStreamMessage;
import com.alibaba.dashscope.threads.runs.Run;
import com.alibaba.dashscope.threads.runs.RunParam;
import com.alibaba.dashscope.threads.runs.RunStepDelta;
import com.alibaba.dashscope.threads.runs.Runs;
import com.alibaba.dashscope.threads.runs.SubmitToolOutputsParam;
import com.alibaba.dashscope.threads.runs.ThreadAndRunParam;
import com.alibaba.dashscope.threads.runs.ToolOutput;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
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
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestStreamRuns {
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
  public void testCreateStreamRun()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String inputFilePath = "./src/test/resources/stream_runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonArray responseEvents = jsonObject.get("create_stream_run_response").getAsJsonArray();
    MockResponse mockResponse = TestUtils.createStreamRunMockResponse(responseEvents, 200);
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
        RunParam.builder().assistantId(assistantId).model(model).instructions(instructions)
            .additionalInstructions(additionalInstructions).additionalMessage(msg2)
            .additionalMessage(msg1)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .metadata(metadata).temperature(temperature).maxCompletionTokens(maxCompletionTokens)
            .maxPromptTokens(maxPromptTokens).truncationStrategy(truncationStrategy)
            .toolChoice(ToolQuarkSearch.builder().build()).stream(true)
            .build();

    Runs runs = new Runs();
    String threadId = UUID.randomUUID().toString();
    Flowable<AssistantStreamMessage> runEvents = runs.createStream(threadId, param);
    AtomicInteger messageCounter = new AtomicInteger();
    runEvents.blockingForEach(
        runEvent -> {
          System.out.println(runEvent.getEvent());
          System.out.println(runEvent.getData());
          messageCounter.incrementAndGet();
        });
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/runs", threadId);
    TestUtils.verifyRequest(request, param, RunParam.class, expectPath, "POST");
    assertEquals(messageCounter.get(), 13);
  }

  @Test
  public void testCreateStreamSubmitRunOutputs()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String inputFilePath = "./src/test/resources/stream_runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonArray responseEvents =
        jsonObject.get("create_stream_require_action_function_1").getAsJsonArray();
    MockResponse mockResponse = TestUtils.createStreamRunMockResponse(responseEvents, 200);
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
        RunParam.builder().assistantId(assistantId).model(model).instructions(instructions)
            .additionalInstructions(additionalInstructions).additionalMessage(msg2)
            .additionalMessage(msg1)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .metadata(metadata).temperature(temperature).maxCompletionTokens(maxCompletionTokens)
            .maxPromptTokens(maxPromptTokens).truncationStrategy(truncationStrategy)
            .toolChoice(ToolQuarkSearch.builder().build()).stream(true)
            .build();

    Runs runs = new Runs();
    String threadId = UUID.randomUUID().toString();
    Flowable<AssistantStreamMessage> runEvents = runs.createStream(threadId, param);
    AtomicInteger messageCounter = new AtomicInteger();
    List<Run> requireActionRuns = new ArrayList<>();
    List<RunStepDelta> runStepDeltas = new ArrayList<>();
    runEvents.blockingForEach(
        runEvent -> {
          System.out.println(runEvent.getEvent());
          System.out.println(runEvent.getData());
          if (runEvent.getEvent().equals(AssistantStreamEvents.THREAD_RUN_REQUIRES_ACTION)) {
            requireActionRuns.add((Run) runEvent.getData());
          }
          if (runEvent.getEvent().equals(AssistantStreamEvents.THREAD_RUN_STEP_DELTA)) {
            runStepDeltas.add((RunStepDelta) runEvent.getData());
          }
          messageCounter.incrementAndGet();
        });
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/runs", threadId);
    TestUtils.verifyRequest(request, param, RunParam.class, expectPath, "POST");
    assertTrue(requireActionRuns.size() == 1);
    String runId = UUID.randomUUID().toString();
    String toolCallId = UUID.randomUUID().toString();
    String toolOutputString = UUID.randomUUID().toString();
    ToolOutput toolOutput =
        ToolOutput.builder().toolCallId(toolCallId).output(toolOutputString).build();
    SubmitToolOutputsParam submitOutputsParam =
        SubmitToolOutputsParam.builder().stream(true).toolOutput(toolOutput).build();
    assertTrue(runStepDeltas.size() == 13);
    // put the summit stream output to mock server.
    responseEvents = jsonObject.get("create_stream_require_action_function_2").getAsJsonArray();
    mockResponse = TestUtils.createStreamRunMockResponse(responseEvents, 200);
    mockServer.enqueue(mockResponse);
    messageCounter.getAndSet(0);
    runEvents = runs.submitStreamToolOutputs(threadId, runId, submitOutputsParam);
    runEvents.blockingForEach(
        runEvent -> {
          System.out.println(runEvent.getEvent());
          System.out.println(runEvent.getData());
          messageCounter.incrementAndGet();
        });
    request = mockServer.takeRequest();
    expectPath = String.format("/api/v1/threads/%s/runs/%s/submit_tool_outputs", threadId, runId);
    TestUtils.verifyRequest(
        request, submitOutputsParam, SubmitToolOutputsParam.class, expectPath, "POST");
    assertTrue(messageCounter.get() == 10);
  }

  @Test
  public void testCreateStreamThreadAndRun()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String inputFilePath = "./src/test/resources/stream_runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonArray responseEvents =
        jsonObject.get("create_stream_thread_and_run_response").getAsJsonArray();
    MockResponse mockResponse = TestUtils.createStreamRunMockResponse(responseEvents, 200);
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
    ThreadAndRunParam param =
        ThreadAndRunParam.builder().thread(threadParam).assistantId(assistantId).model(model)
            .instructions(instructions).additionalInstructions(additionalInstructions)
            .additionalMessage(msg2).additionalMessage(msg1)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .metadata(metadata).temperature(temperature).maxCompletionTokens(maxCompletionTokens)
            .maxPromptTokens(maxPromptTokens).truncationStrategy(truncationStrategy)
            .toolChoice(ToolQuarkSearch.builder().build()).stream(true)
            .build();

    Runs runs = new Runs();
    Flowable<AssistantStreamMessage> runEvents = runs.createStreamThreadAndRun(param);
    AtomicInteger messageCounter = new AtomicInteger();
    List<AssistantStreamMessage> eventMessages = new ArrayList<>();
    runEvents.blockingForEach(
        runEvent -> {
          System.out.println(runEvent.getEvent());
          System.out.println(runEvent.getData());
          messageCounter.incrementAndGet();
          eventMessages.add(runEvent);
        });
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/runs");
    TestUtils.verifyRequest(request, param, RunParam.class, expectPath, "POST");
    assertEquals(messageCounter.get(), 15);
    assertEquals(eventMessages.get(0).getEvent(), AssistantStreamEvents.THREAD_CREATED);
  }

  @Test
  public void testCreateStreamRunCallBack()
      throws IOException, ApiException, NoApiKeyException, InterruptedException,
          InputRequiredException, InvalidateParameter {
    String inputFilePath = "./src/test/resources/stream_runs.json";
    byte[] content = Files.readAllBytes(Paths.get(inputFilePath));
    String jsonContent = new String(content, StandardCharsets.UTF_8);
    JsonObject jsonObject = JsonUtils.parse(jsonContent);
    JsonArray responseEvents =
        jsonObject.get("create_stream_thread_and_run_response").getAsJsonArray();
    MockResponse mockResponse = TestUtils.createStreamRunMockResponse(responseEvents, 200);
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
        RunParam.builder().assistantId(assistantId).model(model).instructions(instructions)
            .additionalInstructions(additionalInstructions).additionalMessage(msg2)
            .additionalMessage(msg1)
            .tools(
                Arrays.asList(
                    buildFunction(), ToolQuarkSearch.builder().build(), ToolWanX.builder().build()))
            .metadata(metadata).temperature(temperature).maxCompletionTokens(maxCompletionTokens)
            .maxPromptTokens(maxPromptTokens).truncationStrategy(truncationStrategy)
            .toolChoice(ToolQuarkSearch.builder().build()).stream(true)
            .build();

    Runs runs = new Runs();
    String threadId = UUID.randomUUID().toString();
    AssistantEventHandlerTest handler = new AssistantEventHandlerTest();
    runs.createStream(threadId, param, handler);
    handler.await();
    assertTrue(handler.isCompleted());
    assertNull(handler.getError());
    assertEquals(handler.getAssistantThread().getId(), "thread_123");
    assertEquals(handler.getFinalMessage().getId(), "msg_001");
    assertEquals(handler.getRun().getId(), "run_123");
    assertEquals(handler.getFinalRunStep().getId(), "step_001");
    RecordedRequest request = mockServer.takeRequest();
    String expectPath = String.format("/api/v1/threads/%s/runs", threadId);
    TestUtils.verifyRequest(request, param, RunParam.class, expectPath, "POST");
  }
}
