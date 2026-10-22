/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;

/**
 * Title qwen tts generation.<br>
 * Description qwen tts generation.<br>
 *
 * @author yuanci.ytb
 * @since 2.19.0
 */

public class MultiModelConversationQvq {
    private static final String MODEL = "qvq-max";

    public static void call() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(Collections.singletonMap("image", "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg"),
                        Collections.singletonMap("text", "请解答这道题")))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .message(userMessage)
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.print(JsonUtils.toJson(result));
    }

    public static void streamCall() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(Collections.singletonMap("image", "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg"),
                        Collections.singletonMap("text", "请解答这道题")))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .message(userMessage)
                .build();

        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
        result.blockingForEach( x -> System.out.println(JsonUtils.toJson(x)));
    }

    public static void callWithCallback() throws ApiException, NoApiKeyException, UploadFileException, InputRequiredException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(Collections.singletonMap("image", "https://img.alicdn.com/imgextra/i1/O1CN01gDEY8M1W114Hi3XcN_!!6000000002727-0-tps-1024-406.jpg"),
                        Collections.singletonMap("text", "请解答这道题")))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MODEL)
                .message(userMessage)
                .build();

        Semaphore semaphore = new Semaphore(0);
        conv.streamCall(param, new ResultCallback<MultiModalConversationResult>() {
            @Override
            public void onEvent(MultiModalConversationResult message) {
                System.out.println(JsonUtils.toJson(message));
            }

            @Override
            public void onComplete() {
                semaphore.release();
            }

            @Override
            public void onError(Exception e) {
                System.out.printf("error: %s", e.getMessage());
                semaphore.release();
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) {
        try {
            call();
//            streamCall();
//            callWithCallback();
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}