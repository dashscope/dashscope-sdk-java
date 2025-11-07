// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.aigc.generation.SearchOptions;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.reactivex.Flowable;

public class GenerationStreamCall {
    public static void streamCall()
            throws NoApiKeyException, ApiException, InputRequiredException {
        GenerationParam param =
                GenerationParam.builder()
                        .model("qwen-turbo")
                        .prompt("你好")
                        .temperature((float) 1.0)
                        .incrementalOutput(false)
                        .repetitionPenalty((float) 1.0)
                        .topK(50)
                        .build();
        System.out.println(param.getHttpBody().toString());
        Generation generation = new Generation();
        Flowable<GenerationResult> flowable = generation.streamCall(param);
        flowable.blockingForEach(message -> {
            System.out.println(JsonUtils.toJson(message));
            Long time = System.currentTimeMillis();
        });
    }

    public static void streamCallWithCallback()
            throws NoApiKeyException, ApiException, InputRequiredException,InterruptedException {
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .model(Generation.Models.QWEN_PLUS)
                .prompt("你好")
                .topP(0.8)
                .incrementalOutput(false)
                .build();
        Semaphore semaphore = new Semaphore(0);
        gen.streamCall(param, new ResultCallback<GenerationResult>() {

            @Override
            public void onEvent(GenerationResult message) {
                System.out.println(message);
            }
            @Override
            public void onError(Exception err){
                System.out.println(String.format("Exception: %s", err.getMessage()));
                semaphore.release();
            }

            @Override
            public void onComplete(){
                System.out.println("Completed");
                semaphore.release();
            }

        });
        semaphore.acquire();

    }

    public static void streamCallWithReasoningContent()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .model("qwen-plus")
                .prompt("1.1和0.9哪个大")
                .topP(0.8)
                .incrementalOutput(false)
                .enableThinking(true)
                .resultFormat("message")
                .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(message -> {
            System.out.println(JsonUtils.toJson(message));
        });
    }


    public static void streamCallWithSearchOptions()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        GenerationParam param = GenerationParam.builder()
                .model(Generation.Models.QWEN_PLUS)
                .prompt("联网搜索明天杭州天气如何？")
                .enableSearch(true)
                .resultFormat("message")
                .searchOptions(SearchOptions.builder()
                        .enableSource(true)
                        .enableCitation(true)
                        .citationFormat("[ref_<number>]")
                        .searchStrategy("pro_max")
                        .forcedSearch(true)
                        .build())
                .build();
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(message -> {
            System.out.println(JsonUtils.toJson(message));
        });
    }

    // Inner classes for tool functions
    public static class GetWeatherTool {
        private String location;

        public GetWeatherTool(String location) {
            this.location = location;
        }

        public String call() {
            return location + "今天是晴天";
        }
    }

    public static class GetTimeTool {
        public GetTimeTool() {
        }

        public String call() {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentTime = "当前时间：" + now.format(formatter) + "。";
            return currentTime;
        }
    }

    public static void streamCallWithToolCalls()
            throws NoApiKeyException, ApiException, InputRequiredException {
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12,
                        OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);
        ObjectNode jsonSchemaWeather = generator.generateSchema(GetWeatherTool.class);
        ObjectNode jsonSchemaTime = generator.generateSchema(GetTimeTool.class);

        FunctionDefinition fdWeather = FunctionDefinition.builder()
                .name("get_current_weather")
                .description("获取指定地区的天气")
                .parameters(JsonUtils.parseString(jsonSchemaWeather.toString())
                        .getAsJsonObject())
                .build();
        FunctionDefinition fdTime = FunctionDefinition.builder()
                .name("get_current_time")
                .description("获取当前时刻的时间")
                .parameters(JsonUtils.parseString(jsonSchemaTime.toString())
                        .getAsJsonObject())
                .build();

        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant. When asked a question, use tools wherever possible.")
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("杭州天气")
                .build();

        List<Message> messages = new ArrayList<>();
        messages.addAll(Arrays.asList(systemMsg, userMsg));

        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("qwen-plus")
                .messages(messages)
                .resultFormat(ResultFormat.MESSAGE)
                .incrementalOutput(false)
                .tools(Arrays.asList(
                        ToolFunction.builder().function(fdWeather).build(),
                        ToolFunction.builder().function(fdTime).build()))
                .build();

        Generation gen = new Generation();
//    GenerationResult result = gen.call(param);
//    System.out.println(JsonUtils.toJson(result));
        Flowable<GenerationResult> result = gen.streamCall(param);
        result.blockingForEach(message -> {
            System.out.println(JsonUtils.toJson(message));
        });
    }

    public static void main(String[] args) {
        try {
            streamCall();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }

//    try {
//      streamCallWithCallback();
//    } catch (ApiException | NoApiKeyException | InputRequiredException | InterruptedException e) {
//      System.out.println(e.getMessage());
//    }

//    try {
//      streamCallWithToolCalls();
//    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
//      System.out.println(e.getMessage());
//    }

//    try {
//      streamCallWithReasoningContent();
//    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
//      System.out.println(e.getMessage());
//    }

//    try {
//      streamCallWithSearchOptions();
//    } catch (ApiException | NoApiKeyException | InputRequiredException e) {
//      System.out.println(e.getMessage());
//    }

        System.exit(0);
    }
}
