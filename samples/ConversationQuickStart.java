// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.List;
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.common.History;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class ConversationQuickStart {
    public static void quickStartWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Conversation conversation = new Conversation();
        List<Message> messageManager = new ArrayList<>();
        messageManager.add(Message.builder().role(Role.USER.getValue()).content("今天天气好吗？").build());
        messageManager.add(Message.builder().role(Role.ASSISTANT.getValue()).content("今天天气不错，要出去玩玩嘛？").build());
        messageManager.add(Message.builder().role(Role.USER.getValue()).content("那你有什么地方推荐？").build());
        ConversationParam param = ConversationParam
        .builder()
        .model(Conversation.Models.QWEN_PLUS)
        .messages(messageManager)
        .topP(0.8)
        .enableSearch(true)
        .resultFormat(ResultFormat.MESSAGE)
        .build();
        ConversationResult result = conversation.call(param);
        System.out.println(result);
    }
    public static void main(String[] args) throws InputRequiredException {
        try {
            quickStartWithMessage();
        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
