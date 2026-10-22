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
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiModalConversationQwenVLOcr {
    private static final String modelName = "qwen-vl-ocr-2025-08-28";
    public static void videoImageListSample() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage systemMessage = MultiModalMessage.builder()
                .role(Role.SYSTEM.getValue())
                .content(Arrays.asList(Collections.singletonMap("text", "You are a helpful assistant.")))
                .build();

        Map<String, Object> imageContent = new HashMap<>();
        imageContent.put("type", "image");
        imageContent.put("image", "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241108/ctdzex/biaozhun.jpg");
        imageContent.put("min_pixels", "401408");
        imageContent.put("max_pixels", "6422528");
        imageContent.put("enable_rotate", false);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("type", "text");
        textContent.put("text", "定位所有的文字行，并且返回旋转矩形([cx, cy, width, height, angle])的坐标结果。");

        JsonObject resultSchema = new JsonObject();
        resultSchema.addProperty("Calories", "");

        OcrOptions ocrOptions = OcrOptions.builder()
                .task(OcrOptions.Task.ADVANCED_RECOGNITION)
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
        System.out.println(JsonUtils.toJson(result));
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
