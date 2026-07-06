import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.alibaba.dashscope.aigc.multimodalconversation.*;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.JsonUtils;

public class MultiModalConversationVLO {
    public static void imageGen() throws ApiException, NoApiKeyException, UploadFileException, IOException {

        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        new HashMap<String, Object>() {{ put("text", "请画一张图片，女人和狗在一起坐在沙滩上。"); }}
                )).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-image")
                .messages(Arrays.asList(userMessage))
                .negativePrompt("低质量")
                .watermark(true)
                .size("928*1664")
                .n(1)
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println(result);
        System.out.println(JsonUtils.toJson(result));
    }

    public static void imageEdit() throws ApiException, NoApiKeyException, UploadFileException, IOException {

        MultiModalConversation conv = new MultiModalConversation();

        MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
                .content(Arrays.asList(
                        new HashMap<String, Object>() {{ put("image", "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"); }},
                        new HashMap<String, Object>() {{ put("text", "请画一张内容相同，风格类似的图片。把女人换成男人"); }}
                )).build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .model("qwen-image-edit")
                .messages(Arrays.asList(userMessage))
                .negativePrompt("低质量")
                .watermark(true)
                .build();

        MultiModalConversationResult result = conv.call(param);
        System.out.println(result);
        System.out.println(JsonUtils.toJson(result));
    }

    public static void main(String[] args) {
        try {
//            imageGen();
            imageEdit();
        } catch (ApiException | NoApiKeyException | UploadFileException | IOException e) {
            System.out.println(e.getMessage());
        }
        System.exit(0);
    }
}