// Copyright (c) Alibaba, Inc. and its affiliates.

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.alibaba.dashscope.aigc.multimodalconversation.*;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.tools.FunctionDefinition;
import com.alibaba.dashscope.tools.ToolFunction;
import com.alibaba.dashscope.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import io.reactivex.Flowable;

public class MultiModalConversationUsage {
    private static final String modelName = "qwen-vl-max-latest";

    public static void simpleMultiModalConversationCall() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessageItemText systemText = new MultiModalMessageItemText("You are a helpful assistant.");
        MultiModalConversationMessage systemMessage = MultiModalConversationMessage.builder()
                .role(Role.SYSTEM.getValue()).content(Arrays.asList(systemText)).build();
        MultiModalMessageItemImage userImage = new MultiModalMessageItemImage(
                "https://data-generator-idst.oss-cn-shanghai.aliyuncs.com/dashscope/image/multi_embedding/image/256_1.png");
        MultiModalMessageItemText userText = new MultiModalMessageItemText("图片里有什么东西?");
        MultiModalConversationMessage userMessage =
                MultiModalConversationMessage.builder().role(Role.USER.getValue())
                        .content(Arrays.asList(userImage, userText)).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MultiModalConversationUsage.modelName).message(systemMessage)
                .vlHighResolutionImages(true)
                .vlEnableImageHwOutput(true)
//                .incrementalOutput(true)
                .message(userMessage).build();
        MultiModalConversationResult result = conv.call(param);
        System.out.println(result);
        System.out.print(JsonUtils.toJson(result));
//        Flowable<MultiModalConversationResult> results = conv.streamCall(param);
//        results.blockingForEach(result -> {
//            System.out.println(JsonUtils.toJson(result));
//        });
    }

    public static void MultiRoundConversationCall() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessageItemText systemText = new MultiModalMessageItemText("You are a helpful assistant.");
        MultiModalConversationMessage systemMessage = MultiModalConversationMessage.builder()
                .role(Role.SYSTEM.getValue()).content(Arrays.asList(systemText)).build();
        MultiModalMessageItemImage userImage = new MultiModalMessageItemImage(
                "https://data-generator-idst.oss-cn-shanghai.aliyuncs.com/dashscope/image/multi_embedding/image/256_1.png");
        MultiModalMessageItemText userText = new MultiModalMessageItemText("图片里有动物吗?");
        MultiModalConversationMessage userMessage =
                MultiModalConversationMessage.builder().role(Role.USER.getValue())
                        .content(Arrays.asList(userImage, userText)).build();
        List<MultiModalConversationMessage> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MultiModalConversationUsage.modelName).messages(messages)
                .build();
        MultiModalConversationResult result = conv.call(param);
        System.out.println(result);
        System.out.println(JsonUtils.toJson(result));
        MultiModalMessage resultMessage = result.getOutput().getChoices().get(0).getMessage();
        MultiModalMessageItemText assistentText = new MultiModalMessageItemText(
                (String) resultMessage.getContent().get(0).get("text"));
        MultiModalConversationMessage assistentMessage = MultiModalConversationMessage.builder()
                .role(Role.ASSISTANT.getValue()).content(Arrays.asList(assistentText)).build();
        messages.add(assistentMessage);
        userText = new MultiModalMessageItemText("图片动物是什么？");
        messages.add(MultiModalConversationMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(userText)).build());
        param.setMessages((List) messages);
        result = conv.call(param);
        System.out.println(result);
        System.out.println(JsonUtils.toJson(result));
    }

    public static void textInTextStreamOut() throws ApiException, NoApiKeyException, UploadFileException, IOException {
        // create jsonschema generator
        SchemaGeneratorConfigBuilder configBuilder =
                new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);
        SchemaGeneratorConfig config = configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                .without(Option.FLATTENED_ENUMS_FROM_TOSTRING).build();
        SchemaGenerator generator = new SchemaGenerator(config);

        // generate jsonSchema of function.
        ObjectNode jsonSchema = generator.generateSchema(GenerationToolChoice.AddFunctionTool.class);

        // call with tools of function call, jsonSchema.toString() is jsonschema String.
        FunctionDefinition fd = FunctionDefinition.builder().name("add").description("add two number")
                .parameters(JsonUtils.parseString(jsonSchema.toString()).getAsJsonObject()).build();

        // build system message
        MultiModalMessage systemMsg = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", "You are a helpful assistant. When asked a question, use tools wherever possible.")))
                .build();

        // user message to call function.
        MultiModalMessage userMsg = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", "Add 1234 and 4321, Add 2345 and 5432")))
                .build();

        ToolFunction toolFunction = ToolFunction.builder().function(FunctionDefinition.builder().name("add").build()).build();

        MultiModalConversation conversation = new MultiModalConversation();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-vl-max-latest")
                .messages(Arrays.asList(systemMsg, userMsg))
                .modalities(Collections.singletonList("text"))
                .toolChoice(toolFunction)
                .tools(Arrays.asList(ToolFunction.builder().function(fd).build()))
                .parallelToolCalls(true)
                .build();
        Flowable<MultiModalConversationResult> results = conversation.streamCall(param);

        results.blockingForEach(result -> {
            System.out.println(JsonUtils.toJson(result));
        });
    }

    public static void textInAudioStreamOut() throws ApiException, NoApiKeyException, UploadFileException, IOException {
        MultiModalConversation conversation = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Collections.singletonList(Collections.singletonMap("text", "1+1等于几?"))).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-omni-turbo-latest")
                .messages(Collections.singletonList(userMessage))
                .modalities(Collections.singletonList("audio"))
                .audio(AudioParameters.builder()
                        .voice(AudioParameters.Voice.ETHAN)
                        .build())
                .build();
        Flowable<MultiModalConversationResult> results = conversation.streamCall(param);

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(Paths.get("out.pcm")))) {
            results.blockingForEach(result -> {
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> map = content.get(0);
                    if (map.containsKey("text")) {
                        String text = String.valueOf(content.get(0).get("text"));
                        System.out.print(text);
                    } else if (map.containsKey("audio")) {
                        Map<String, Object> audio = (Map<String, Object>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode((String) audio.get("data"));
                        Long expiresAt = (Long) audio.get("expires_at");

                        System.out.printf("write [%d] audio data to file, expires at: %d\n", data.length, expiresAt);
                        os.write(data);
                    }
                }
            });
        }
    }

    public static void audioInTextAudioStreamOut() throws ApiException, NoApiKeyException, UploadFileException, IOException {
        MultiModalConversation conversation = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(Collections.singletonMap("audio", "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav"),
                        Collections.singletonMap("text", "这段音频在说什么?"))).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-omni-turbo-latest")
                .messages(Collections.singletonList(userMessage))
                .modalities(Arrays.asList("text", "audio"))
                .audio(AudioParameters.builder()
                        .voice(AudioParameters.Voice.ETHAN)
                        .build())
                .build();
        Flowable<MultiModalConversationResult> results = conversation.streamCall(param);

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(Paths.get("out.pcm")))) {
            results.blockingForEach(result -> {
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> map = content.get(0);
                    if (map.containsKey("text")) {
                        String text = String.valueOf(content.get(0).get("text"));
                        System.out.print(text);
                    } else if (map.containsKey("audio")) {
                        Map<String, Object> audio = (Map<String, Object>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode((String) audio.get("data"));
                        Long expiresAt = (Long) audio.get("expires_at");

                        System.out.printf("write [%d] audio data to file, expires at: %d\n", data.length, expiresAt);
                        os.write(data);
                    }
                }
            });
        }
    }

    public static void imageInTextAudioStreamOut() throws ApiException, NoApiKeyException, UploadFileException, IOException {
        MultiModalConversation conversation = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(Collections.singletonMap("image", "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"),
                        Collections.singletonMap("text", "这是什么"))).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-omni-turbo-latest")
                .messages(Collections.singletonList(userMessage))
                .modalities(Arrays.asList("text", "audio"))
                .audio(AudioParameters.builder()
                        .voice(AudioParameters.Voice.ETHAN)
                        .build())
                .build();
        Flowable<MultiModalConversationResult> results = conversation.streamCall(param);

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(Paths.get("out.pcm")))) {
            results.blockingForEach(result -> {
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> map = content.get(0);
                    if (map.containsKey("text")) {
                        String text = String.valueOf(content.get(0).get("text"));
                        System.out.print(text);
                    } else if (map.containsKey("audio")) {
                        Map<String, Object> audio = (Map<String, Object>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode((String) audio.get("data"));
                        Long expiresAt = (Long) audio.get("expires_at");

                        System.out.printf("write [%d] audio data to file, expires at: %d\n", data.length, expiresAt);
                        os.write(data);
                    }
                }
            });
        }
    }

    public static void videoInTextAudioStreamOut() throws ApiException, NoApiKeyException, UploadFileException, IOException {
        MultiModalConversation conversation = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(Collections.singletonMap("video",
                                Arrays.asList("https://dashscope.oss-cn-beijing.aliyuncs.com/images/tiger.png")),
                        Collections.singletonMap("text", "描述这个视频的具体过程"))).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-omni-turbo-latest")
                .messages(Collections.singletonList(userMessage))
                .modalities(Arrays.asList("text", "audio"))
                .audio(AudioParameters.builder()
                        .voice(AudioParameters.Voice.ETHAN)
                        .build())
                .build();
        Flowable<MultiModalConversationResult> results = conversation.streamCall(param);

        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(Paths.get("out.pcm")))) {
            results.blockingForEach(result -> {
                List<Map<String, Object>> content = result.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    Map<String, Object> map = content.get(0);
                    if (map.containsKey("text")) {
                        String text = String.valueOf(content.get(0).get("text"));
                        System.out.print(text);
                    } else if (map.containsKey("audio")) {
                        Map<String, Object> audio = (Map<String, Object>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode((String) audio.get("data"));
                        Long expiresAt = (Long) audio.get("expires_at");

                        System.out.printf("write [%d] audio data to file, expires at: %d\n", data.length, expiresAt);
                        os.write(data);
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        try {
            simpleMultiModalConversationCall();
//            MultiRoundConversationCall();
//            textInAudioStreamOut();
//            textInTextStreamOut();
//            audioInTextAudioStreamOut();
//            imageInTextAudioStreamOut();
//            videoInTextAudioStreamOut();
        } catch (ApiException | NoApiKeyException | UploadFileException /*| IOException*/ e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
