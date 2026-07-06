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
import java.util.HashMap;

public class QwenASRUsage {
    private static final String modelName = "qwen3-asr-flash";
    public static void streamCall()
            throws ApiException, NoApiKeyException, UploadFileException {
        MultiModalConversation conv = new MultiModalConversation();
        // must create mutable map.
        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(new HashMap<String, Object>(){{put("audio", "https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3");}}
                )).build();
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model(modelName)
                .message(userMessage)
                .parameter("asr_options", new HashMap<String, Object>(){{put("enable_lid", true); put("language", "zh");}})
                .incrementalOutput(true)
                .build();
        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
        result.blockingForEach(item -> {
            if (item.getOutput() == null) {
                return;
            }
            try {
                System.out.println(item.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text"));
            } catch (Exception e){
                System.exit(0);
            }
        });
    }
    public static void main(String[] args) {
        try {
            streamCall();
        } catch (ApiException | NoApiKeyException | UploadFileException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}
