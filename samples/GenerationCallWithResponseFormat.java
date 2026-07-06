// Copyright (c) Alibaba, Inc. and its affiliates.
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.ResponseFormat;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.util.ArrayList;
import java.util.List;


public class GenerationCallWithResponseFormat {
    public static void callWithJsonObject()
            throws NoApiKeyException, ApiException, InputRequiredException {
        Generation gen = new Generation();
        List<Message> msgManager = new ArrayList<>();
        Message systemMessage = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();
        Message userMessage = Message.builder()
                .role(Role.USER.getValue())
                .content("请将userId=123, userName=test按照json格式输出")
                .build();
        msgManager.add(systemMessage);
        msgManager.add(userMessage);
        GenerationParam param =
                GenerationParam.builder().model(Generation.Models.QWEN_PLUS).messages(msgManager)
                        .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                        .responseFormat(ResponseFormat.from(ResponseFormat.JSON_OBJECT))
                        .build();

        GenerationResult result = gen.call(param);
        System.out.println(JsonUtils.toJson(result));
    }


    public static void main(String[] args) {
        try {
            callWithJsonObject();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
