import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TestQwen3VlPlusFullOutput {

    private static final String IMAGE_URL =
            "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg";

    public static void main(String[] args) throws Exception {
        String apiKey = args.length > 0 ? args[0] : System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Usage: java TestQwen3VlPlusFullOutput <api-key>");
            System.err.println("Or set DASHSCOPE_API_KEY environment variable.");
            System.exit(1);
        }

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("image", IMAGE_URL),
                        Collections.singletonMap("text", "图中描绘的是什么景象？")))
                .build();

        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                //.model("qwen3-vl-plus")
                .model("qwen-vl-max")
                .messages(Arrays.asList(userMessage))
                //.incrementalOutput(false)
                .build();

        System.out.println("=== qwen3-vl-plus | incrementalOutput=false | streamCall ===\n");

        List<String> fragments = new ArrayList<>();
        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
        result.blockingForEach(item -> {
            try {
                List<Map<String, Object>> content = item.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    Object textObj = content.get(0).get("text");
                    String text = textObj == null ? "" : textObj.toString();
                    if (!text.isEmpty()) {
                        fragments.add(text);
                        int preview = Math.min(text.length(), 80);
                        System.out.printf("[chunk %d] len=%d -> %s%n",
                                fragments.size(), text.length(),
                                text.substring(0, preview).replace("\n", "\\n"));
                    }
                }
            } catch (Exception e) {
                System.err.println("Parse error: " + e.getMessage());
            }
        });

        System.out.println("\n--- Summary ---");
        System.out.println("Total chunks: " + fragments.size());

        if (!fragments.isEmpty()) {
            boolean monotonic = true;
            for (int i = 1; i < fragments.size(); i++) {
                if (fragments.get(i).length() < fragments.get(i - 1).length()) {
                    monotonic = false;
                    break;
                }
            }
            System.out.println("Lengths monotonically non-decreasing: " + monotonic);
            System.out.println("Output type: " + (monotonic ? "FULL (expected)" : "INCREMENTAL (unexpected)"));
            System.out.println("\nFinal text:\n" + fragments.get(fragments.size() - 1));
        }
    }
}
