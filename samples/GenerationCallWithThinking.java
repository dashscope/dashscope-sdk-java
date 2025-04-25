// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import io.reactivex.Flowable;


public class GenerationCallWithThinking {
    static final String MODE_NAME = System.getenv("MODEL_NAME");

    public static void streamCallWithThinking()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg =
                Message.builder().role(Role.SYSTEM.getValue()).content("You are a helpful assistant.").build();
        Message userMsg = Message.builder().role(Role.USER.getValue()).content("9.9和9.11谁大").build();
        GenerationParam param = GenerationParam.builder().model(MODE_NAME)
                .enableThinking(true)
                .thinkingBudget(10000)
                .logprobs(true)
                .topLogprobs(1)
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true)
                .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingSubscribe( data -> {
            System.out.println(JsonUtils.toJson(data));
        });
    }

    public static void streamCallWithToolChoice()
            throws NoApiKeyException, ApiException, InputRequiredException {
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // generate jsonSchema of function.
        ObjectNode jsonSchema = generator.generateSchema(GenerationToolChoice.AddFunctionTool.class);

        // call with tools of function call, jsonSchema.toString() is jsonschema String.
        FunctionDefinition fd = FunctionDefinition.builder()
                .name("add").description("add two number")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();

        ToolFunction toolFunction = ToolFunction.builder().function(FunctionDefinition.builder().name("add").build()).build();


        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant. When asked a question, use tools wherever possible.")
                .build();
        Message userMsg = Message.builder().role(Role.USER.getValue()).content("Add 32393 and 88909").build();
        GenerationParam param = GenerationParam.builder().model(MODE_NAME)
                .enableThinking(true)
                .toolChoice("auto") // NOTE: tool_choice parameter must be "auto" or "none" when enable_thinking is true
//                .toolChoice(toolFunction)
                .tools(Arrays.asList(ToolFunction.builder().function(fd).build()))
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true)
                .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingSubscribe( data -> {
            System.out.println(JsonUtils.toJson(data));
        });
    }


    public static void streamCallWithPartial()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        List<Message> msgManager = new ArrayList<>();
        Message systemMsg = Message.builder()
                .role(Role.ASSISTANT.getValue())
                .content("春天来了，大地")
//                .partial(true) // NOTE: Partial mode is not supported when enable_thinking is true
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("很久很久以前，有一只小猫，猫的名字叫小花。")
                .build();
        msgManager.add(userMsg);
        msgManager.add(systemMsg);
        GenerationParam param =
                GenerationParam.builder()
                        .model(MODE_NAME)
                        .messages(msgManager)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .topP(0.8)
                        .incrementalOutput(true)
                        .enableThinking(true)
                        .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingSubscribe( data -> {
            System.out.println(JsonUtils.toJson(data));
        });
    }

    public static void main(String[] args){
        try {
            streamCallWithThinking();
//            streamCallWithToolChoice();
//            streamCallWithPartial();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
