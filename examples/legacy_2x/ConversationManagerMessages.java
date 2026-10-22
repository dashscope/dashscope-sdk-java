// Copyright (c) Alibaba, Inc. and its affiliates.

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.alibaba.dashscope.aigc.conversation.Conversation;
import com.alibaba.dashscope.aigc.conversation.ConversationParam;
import com.alibaba.dashscope.aigc.conversation.ConversationResult;
import com.alibaba.dashscope.aigc.conversation.ConversationParam.ResultFormat;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import io.reactivex.Flowable;

public class ConversationManagerMessages {
    public static void callWithMessagesStream() throws ApiException, NoApiKeyException, InputRequiredException {
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
        .resultFormat(ResultFormat.MESSAGE) // set the result format with messages.
        .enableSearch(true)
        .build();
        AtomicReference<ConversationResult> finalResult = new AtomicReference<ConversationResult>();
        try{
            Flowable<ConversationResult> result = conversation.streamCall(param);
            result.blockingForEach(msg->{
                System.out.println(msg);
                finalResult.set(msg);
            });
        }catch(ApiException ex){
            System.out.println(ex.getMessage());
        }
        messageManager.add(finalResult.get().getOutput().getChoices().get(0).getMessage());
        Message message = Message.builder().role(Role.USER.getValue()).content("那个公园最近？").build();
        messageManager.add(message);
        param.setMessages(messageManager);
        ConversationResult result = conversation.call(param);
        System.out.println(result);
    }
    public static void main(String[] args) {
        try {
            callWithMessagesStream();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
