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
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

public class MultiModalConversationUsage {
    private static final String modelName = "qwen-vl-chat-v1";

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
                .message(userMessage).build();
        MultiModalConversationResult result = conv.call(param);
        System.out.print(result);
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
        System.out.print(result);
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
                        Map<String, String> audio = (Map<String, String>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode(audio.get("data"));

                        System.out.printf("write [%d] audio data to file\n", data.length);
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
                        Map<String, String> audio = (Map<String, String>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode(audio.get("data"));

                        System.out.printf("write [%d] audio data to file\n", data.length);
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
                        Map<String, String> audio = (Map<String, String>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode(audio.get("data"));

                        System.out.printf("write [%d] audio data to file\n", data.length);
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
                        Map<String, String> audio = (Map<String, String>) map.get("audio");
                        byte[] data = Base64.getDecoder().decode(audio.get("data"));

                        System.out.printf("write [%d] audio data to file\n", data.length);
                        os.write(data);
                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        try {
//            simpleMultiModalConversationCall();
//            MultiRoundConversationCall();
            textInAudioStreamOut();
//            audioInTextAudioStreamOut();
//            imageInTextAudioStreamOut();
//            videoInTextAudioStreamOut();
        } catch (ApiException | NoApiKeyException | UploadFileException | IOException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
