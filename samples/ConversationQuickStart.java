// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.common.History;
import com.alibaba.dashscope.common.HistoryManager;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;

public class ConversationQuickStart {
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
    public static void main(String[] args) {
        try {
            quickStart();
        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
