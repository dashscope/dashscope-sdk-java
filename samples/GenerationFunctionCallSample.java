// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationOutput.Choice;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolCallBase;
import com.alibaba.dashscope.tools.ToolCallFunction;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;

public class GenerationFunctionCallSample {

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


  public static void callFunctionAdd()
      throws NoApiKeyException, ApiException, InputRequiredException {
    // create jsonschema generator
    SchemaGeneratorConfigBuilder configBuilder =
        new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
    SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
        .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
    SchemaGenerator generator = new SchemaGenerator(config);

    // generate jsonSchema of function.
    ObjectNode jsonSchema = generator.generateSchema(AddFunctionTool.class);

    // call with tools of function call, jsonSchema.toString() is jsonschema String.
    FunctionDefinition fd = FunctionDefinition.builder().name("mycalc").description("calc")
        .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();

    // build system message
    Message systemMsg = Message.builder().role(Role.SYSTEM.getValue())
        .content("You are a helpful assistant. When asked a question, use tools wherever possible.")
        .build();

    // user message to call function.
    Message userMsg =
        Message.builder().role(Role.USER.getValue()).content("Call mycalc 32393 and 88909").build();

    // messages to store message request and response.
    List<Message> messages = new ArrayList<>();
    messages.addAll(Arrays.asList(systemMsg, userMsg));

    // create generation call parameter
    GenerationParam param = GenerationParam.builder().model("qwen-max")
        .messages(messages).resultFormat(ResultFormat.MESSAGE)
        .tools(Arrays.asList(ToolFunction.builder().function(fd).build())).build();

    // call the Generation
    Generation gen = new Generation();
    GenerationResult result = gen.call(param);
    // print the result.
    System.out.println(JsonUtils.toJson(result));

    // process the response
    for (Choice choice : result.getOutput().getChoices()) {
      // add the assistant message to list for next Generation call.
      messages.add(choice.getMessage());
      // check if we need call tool.
      if (result.getOutput().getChoices().get(0).getMessage().getToolCalls() != null) {
        // iterator the tool calls
        for (ToolCallBase toolCall : result.getOutput().getChoices().get(0).getMessage()
            .getToolCalls()) {
          // get function call.
          if (toolCall.getType().equals("function")) {
            // get function call name and argument, both String.
            String functionName = ((ToolCallFunction) toolCall).getFunction().getName();
            String functionArgument = ((ToolCallFunction) toolCall).getFunction().getArguments();
            if (functionName.equals("mycalc")) {
              // Create the function object.
              AddFunctionTool addFunction =
                  JsonUtils.fromJson(functionArgument, AddFunctionTool.class);
              // call function.
              int sum = addFunction.call();
              // create the tool message
              Message toolResultMessage = Message.builder().role("tool")
                  .content(String.valueOf(sum)).toolCallId(toolCall.getId()).build();
              // add the tool message to messages list.
              messages.add(toolResultMessage);
              System.out.println(sum);
            }
          }
        }
      }
    }
    // new Generation call with messages include tool result.
    param.setMessages(messages);
    result = gen.call(param);
    System.out.println(JsonUtils.toJson(result));
  }


  public static void main(String[] args) {
    try {
      callFunctionAdd();
    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
      System.out.println(String.format("Exception %s", e.getMessage()));
    }
    System.exit(0);
  }
}
