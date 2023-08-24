// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.common.History;
import com.alibaba.dashscope.common.HistoryManager;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.MessageManager;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class ConversationQuickStart {
    @Deprecated
    public static void quickStart() throws ApiException, NoApiKeyException {
        Conversation conversation = new Conversation();

        HistoryManager historyManager = new HistoryManager(10);
        History his = History.builder().user("今天天气好吗？").bot("今天天气不错，要出去玩玩嘛？").build();
        historyManager.add(his);
        String prompt = "那你有什么地方推荐？";
        ConversationParam param = ConversationParam
        .builder()
        .model(Conversation.Models.QWEN_V1)
        .prompt(prompt)
        .history(historyManager.get())
        .topP(0.8)
        .enableSearch(true)
        .build();
        ConversationResult result = conversation.call(param);
        System.out.println(result);
    }
    public static void quickStartWithMessage() throws ApiException, NoApiKeyException {
        Conversation conversation = new Conversation();
        MessageManager messageManager = new MessageManager(10);
        messageManager.add(Message.builder().role(Role.USER.getValue()).content("今天天气好吗？").build());
        messageManager.add(Message.builder().role(Role.ASSISTANT.getValue()).content("今天天气不错，要出去玩玩嘛？").build());
        messageManager.add(Message.builder().role(Role.USER.getValue()).content("那你有什么地方推荐？").build());
        ConversationParam param = ConversationParam
        .builder()
        .model(Conversation.Models.QWEN_PLUG_V1)
        .messages(messageManager.get())
        .topP(0.8)
        .enableSearch(true)
        .resultFormat(ResultFormat.MESSAGE)
        .build();
        ConversationResult result = conversation.call(param);
        System.out.println(result);
    }
    public static void main(String[] args) {
        try {
            quickStartWithMessage();
            quickStart();
        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
