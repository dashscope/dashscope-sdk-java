// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.aigc.multimodalconversation.AudioParameters;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;

public class MultiModalConversationOmni {
    private static final String modelName = "qwen-omni-turbo";
    public static void videoImageListSample() throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage systemMessage = MultiModalMessage.builder().role(Role.SYSTEM.getValue())
        .content(Arrays.asList(Collections.singletonMap("text", "You are a helpful assistant."))).build();
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
//        .content(Arrays.asList(Collections.singletonMap("audio", "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav"),
//                               Collections.singletonMap("text", "音频里说什么？"))).build();
//          .content(Arrays.asList(Collections.singletonMap("video", Arrays.asList("https://img.alicdn.com/imgextra/i3/O1CN01K3SgGo1eqmlUgeE9b_!!6000000003923-0-tps-3840-2160.jpg",
//                        "https://img.alicdn.com/imgextra/i4/O1CN01BjZvwg1Y23CF5qIRB_!!6000000003000-0-tps-3840-2160.jpg",
//                        "https://img.alicdn.com/imgextra/i4/O1CN01Ib0clU27vTgBdbVLQ_!!6000000007859-0-tps-3840-2160.jpg",
//                        "https://img.alicdn.com/imgextra/i1/O1CN01aygPLW1s3EXCdSN4X_!!6000000005710-0-tps-3840-2160.jpg")),
//                        Collections.singletonMap("text", "描述这个视频的具体过程"))).build();
          .content(Arrays.asList(Collections.singletonMap("image", "https://data-generator-idst.oss-cn-shanghai.aliyuncs.com/dashscope/image/multi_embedding/image/video1.jpg"),
                 Collections.singletonMap("text", "描述图片里的内容"))).build();


        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .messages(Collections.singletonList(userMessage))
                .modalities(Arrays.asList("text", "audio"))
                .audio(AudioParameters.builder().voice(AudioParameters.Voice.CHERRY).build())
                .model(MultiModalConversationOmni.modelName).build();

        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
        result.blockingForEach(data -> {
            System.out.printf("output=%s\n", data.getOutput());
            System.out.printf("usage=%s\n\n", data.getUsage());
        });
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
