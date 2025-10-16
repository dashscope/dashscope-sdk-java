// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
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

public class MultiModalConversationQwenVL {
    private static final String modelName = System.getenv("MODEL_NAME");

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
            DateTimeFormatter formatter = 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentTime = "当前时间：" + now.format(formatter) + "。";
            return currentTime;
        }
    }
    public static void videoImageListSample() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage systemMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
        .content(Arrays.asList(Collections.singletonMap("text", "You are a helpful assistant."))).build();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
        .content(Arrays.asList(Collections.singletonMap("video", Arrays.asList("https://img.alicdn.com/imgextra/i3/O1CN01K3SgGo1eqmlUgeE9b_!!6000000003923-0-tps-3840-2160.jpg",
        "https://img.alicdn.com/imgextra/i4/O1CN01BjZvwg1Y23CF5qIRB_!!6000000003000-0-tps-3840-2160.jpg",
        "https://img.alicdn.com/imgextra/i4/O1CN01Ib0clU27vTgBdbVLQ_!!6000000007859-0-tps-3840-2160.jpg",
        "https://img.alicdn.com/imgextra/i1/O1CN01aygPLW1s3EXCdSN4X_!!6000000005710-0-tps-3840-2160.jpg")), 
        Collections.singletonMap("text", "描述这个视频的具体过程"))).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MultiModalConversationQwenVL.modelName != null ? 
                       MultiModalConversationQwenVL.modelName : "qwen3-vl-30b-a3b-thinking")
//                .enableThinking(true)
//                .thinkingBudget(20)
                .incrementalOutput(false)
                .message(systemMessage)
                .message(userMessage).build();
//        MultiModalConversationResult result = conv.call(param);
//        System.out.print(result);

        Flowable<MultiModalConversationResult> results = conv.streamCall(param);
        results.blockingForEach(result -> {
            System.out.println(JsonUtils.toJson(result));
        });
    }

    public static void streamCallWithToolCalls()
            throws NoApiKeyException, ApiException, UploadFileException {
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

        MultiModalMessage systemMsg = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", 
                    "You are a helpful assistant. When asked a question, use tools wherever possible.")))
                .build();
        MultiModalMessage userMsg = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", 
                    "杭州天气怎么样？现在几点了？")))
                .build();

        List<MultiModalMessage> messages = new ArrayList<>();
        messages.addAll(Arrays.asList(systemMsg, userMsg));

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                // 此处以qwen-vl-max-latest为例，可按需更换模型名称
                .model(MultiModalConversationQwenVL.modelName != null ? 
                       MultiModalConversationQwenVL.modelName : "qwen-vl-max-latest")
                .messages(messages)
                .modalities(Collections.singletonList("text"))
                .incrementalOutput(false)
                .tools(Arrays.asList(
                        ToolFunction.builder().function(fdWeather).build(),
                        ToolFunction.builder().function(fdTime).build()))
                .parallelToolCalls(true)
                .build();

        MultiModalConversation conv = new MultiModalConversation();
        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
        result.blockingForEach(message -> {
            System.out.println(JsonUtils.toJson(message));
        });
    }

    public static void main(String[] args) {
        try {
            videoImageListSample();
//            streamCallWithToolCalls();
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
