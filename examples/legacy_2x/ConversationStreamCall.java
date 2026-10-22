// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

public class ConversationStreamCall {
    public static void testStreamCall() throws ApiException, NoApiKeyException, InputRequiredException {
        Conversation conversation = new Conversation();
        String prompt = "就当前的海洋污染的情况，写一份限塑的倡议书提纲，需要有理有据地号召大家克制地使用塑料制品";
        ConversationParam param = ConversationParam
        .builder()
        .model(Conversation.Models.QWEN_PLUS)
        .prompt(prompt)
        .topP(0.8)
        .enableSearch(true)
        .incrementalOutput(true)
        .build();
        try{
            Flowable<ConversationResult> result = conversation.streamCall(param);
            result.blockingForEach(msg->{
                System.out.print(msg);
            });
        }catch(ApiException ex){
            System.out.println(ex.getMessage());
        }
    }
    public static void main(String[] args) throws InputRequiredException{
        try {
            testStreamCall();
        } catch (ApiException | NoApiKeyException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
