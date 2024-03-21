package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolFunction;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public class TestTools {
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

  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;

  @BeforeEach
  public void before() {
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  @Test
  public void testCallFunction()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    // create jsonschema generator
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

    // build system message
    Message systemMsg =
        Message.builder()
            .role(Role.SYSTEM.getValue())
            .content(
                "You are a helpful assistant. When asked a question, use tools wherever possible.")
            .build();

    // user message to call function.
    Message userMsg =
        Message.builder().role(Role.USER.getValue()).content("Add 32393 and 88909").build();

    // messages to store message request and response.
    List<Message> messages = new ArrayList<>();
    messages.addAll(Arrays.asList(systemMsg, userMsg));

    // create generation call parameter
    GenerationParam param =
        GenerationParam.builder()
            .model(Generation.Models.QWEN_MAX)
            .messages(messages)
            .resultFormat(ResultFormat.MESSAGE)
            .tools(Arrays.asList(ToolFunction.builder().function(fd).build()))
            .build();

    JsonObject toolCallOutput =
        JsonUtils.parse(
            "{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{\"role\":\"assistant\",\"tool_calls\":[{\"function\":{\"name\":\"add\",\"arguments\":\"{\\\"left\\\": 32393, \\\"right\\\": 88909}\"},\"id\":\"\",\"type\":\"function\"}],\"content\":\"\"}}]}");
    JsonObject toolCallUsage =
        JsonUtils.parse("{\"total_tokens\":65,\"output_tokens\":35,\"input_tokens\":30}");
    TestResponse rsp =
        TestResponse.builder()
            .requestId("bf321b27-a3ff-9674-a70e-be5f40a435e4")
            .output(toolCallOutput)
            .usage(toolCallUsage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));

    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    // call the Generation
    Generation gen = new Generation();
    GenerationResult result = gen.call(param);
    assertEquals(result.getOutput().getChoices().get(0).getFinishReason(), "tool_calls");
    assertEquals(
        result.getOutput().getChoices().get(0).getMessage().getToolCalls().get(0).getType(),
        "function");
    ToolCallFunction callFunction =
        (ToolCallFunction)
            result.getOutput().getChoices().get(0).getMessage().getToolCalls().get(0);
    assertEquals(callFunction.getFunction().getArguments(), "{\"left\": 32393, \"right\": 88909}");
    System.out.println(result);
    RecordedRequest request = server.takeRequest();
    String body = request.getBody().readUtf8();
    JsonObject req = JsonUtils.parse(body);
    System.out.println(req);
    assertEquals(req.get("input").getAsJsonObject().get("messages").getAsJsonArray().size(), 2);
    assertEquals(
        req.get("parameters")
            .getAsJsonObject()
            .get("tools")
            .getAsJsonArray()
            .get(0)
            .getAsJsonObject()
            .get("function")
            .getAsJsonObject()
            .get("name")
            .getAsString(),
        "add");
    System.out.println(body);
    // verify result.
  }
}
