// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.aigc.multimodalconversation.OcrOptions;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiModalConversationQwenVLOcr {
    private static final String modelName = "qwen-vl-ocr-2025-02-18";
    public static void videoImageListSample() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Arrays.asList(Collections.singletonMap("text", "You are a helpful assistant.")))
                .build();

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image");
        imageContent.put("image", "http://duguang-llm.oss-cn-hangzhou.aliyuncs.com/llm_data_keeper/public_data/POIE/test_subset/nf0986.jpg");
        imageContent.put("min_pixels", "3136");
        imageContent.put("max_pixels", "2007040");
        imageContent.put("enable_rotate", false);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "提取图像中的文字。");

        JsonObject resultSchema = new JsonObject();
        resultSchema.addProperty("Calories", "");

        OcrOptions ocrOptions = OcrOptions.builder()
                .task(OcrOptions.Task.KEY_INFORMATION_EXTRACTION)
                .taskConfig(OcrOptions.TaskConfig.builder()
                        .resultSchema(resultSchema)
                        .build())
                .build();


        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        imageContent,
                        textContent))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(MultiModalConversationQwenVLOcr.modelName)
                    .message(systemMessage)
                    .message(userMessage)
                .ocrOptions(ocrOptions)
//                .incrementalOutput(true)
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println(result);
//        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
//        result.blockingForEach(System.out::println);
    }

    public static void main(String[] args) {
        try {
            videoImageListSample();
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
