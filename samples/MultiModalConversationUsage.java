// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationMessage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemImage;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalMessageItemText;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;

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
                (String)resultMessage.getContent().get(0).get("text"));
        MultiModalConversationMessage assistentMessage = MultiModalConversationMessage.builder()
                .role(Role.ASSISTANT.getValue()).content(Arrays.asList(assistentText)).build();
        messages.add(assistentMessage);
        userText = new MultiModalMessageItemText("图片动物是什么？");
        messages.add(MultiModalConversationMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(userText)).build());
        param.setMessages((List)messages);
        result = conv.call(param);
        System.out.print(result);
    }

    public static void main(String[] args) {
        try {
            simpleMultiModalConversationCall();
            MultiRoundConversationCall();
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
