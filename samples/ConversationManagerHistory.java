// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.common.History;
import com.alibaba.dashscope.common.HistoryManager;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

public class ConversationManagerHistory {
    @Deprecated
    public static void callWithHistoryStream() throws ApiException, NoApiKeyException {
        Conversation conversation = new Conversation();

        HistoryManager historyManager = new HistoryManager(10);
        History his = History.builder().bot("今天天气不错，要出去玩玩嘛？").user("今天天气好吗？").build();
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
        StringBuilder finalResult = new StringBuilder();
        try{
            Flowable<ConversationResult> result = conversation.streamCall(param);
            result.blockingForEach(msg->{
                System.out.print(msg);
                finalResult.delete(0, finalResult.length());
                finalResult.append(msg.getOutput().getText());
            });
        }catch(ApiException ex){
            System.out.println(ex.getMessage());
        }
        his = History.builder().user(prompt).bot(finalResult.toString()).build();
        historyManager.add(his);
        prompt = "那个公园最近？";
        param.setHistory(historyManager.get());
        param.setPrompt(prompt);
        ConversationResult result = conversation.call(param);
        System.out.println(result);
    }
    public static void main(String[] args) {
        try {
            callWithHistoryStream();
        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
