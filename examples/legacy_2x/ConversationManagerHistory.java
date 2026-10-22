// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.List;
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.common.History;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

public class ConversationManagerHistory {
    @Deprecated
    public static void callWithHistoryStream() throws ApiException, NoApiKeyException, InputRequiredException {
        Conversation conversation = new Conversation();

        List<History> historyManager = new ArrayList<>();
        History his = History.builder().bot("今天天气不错，要出去玩玩嘛？").user("今天天气好吗？").build();
        historyManager.add(his);
        String prompt = "那你有什么地方推荐？";
        ConversationParam param = ConversationParam
        .builder()
        .model(Conversation.Models.QWEN_TURBO)
        .prompt(prompt)
        .history(historyManager)
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
        param.setHistory(historyManager);
        param.setPrompt(prompt);
        ConversationResult result = conversation.call(param);
        System.out.println(result);
    }
    public static void main(String[] args) {
        try {
            callWithHistoryStream();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
