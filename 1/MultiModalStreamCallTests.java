package com.example.linkcheck4j;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MultiModalStreamCallTests {

    private static final Logger logger = LoggerFactory.getLogger(MultiModalStreamCallTests.class);

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    private static final String IMAGE_URL =
            "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241022/emyrja/dog_and_girl.jpeg";

    // ==================== 测试用例 ====================

    /**
     * 场景1: qwen3-vl-plus 开启思考 + 增量输出(true)
     * 预期: 正常增量输出，每次回调只返回新增片段
     */
    @Test
    void qwen3VlPlus_thinking_incrementalOutputTrue_shouldSucceed()
            throws ApiException, NoApiKeyException, UploadFileException {
        StreamResult result = doStreamCall("qwen3-vl-plus", true, true,
                "qwen3-vl-plus | 开思考 | 增量输出(true)");
        assertSuccessfulOutput(result);
        assertIncrementalPattern(result, "qwen3-vl-plus 开思考增量输出");
    }

    /**
     * 场景2: qwen3-vl-plus 开启思考 + 全量输出(false)"
     * 实测: qwen3 系列开思考时不支持全量，实际仍按增量返回
     */
    @Test
    void qwen3VlPlus_thinking_incrementalOutputFalse_shouldFallbackToIncremental()
            throws ApiException, NoApiKeyException, UploadFileException {
        StreamResult result = doStreamCall("qwen3-vl-plus", true, false,
                "qwen3-vl-plus | 开思考 | 全量输出(false) -> 实际仍为增量");
        assertSuccessfulOutput(result);
    }

    /**
     * 场景3: qwen3-vl-plus 不开思考 + 增量输出(true)
     * 预期: 正常增量输出
     */
    @Test
    void qwen3VlPlus_noThinking_incrementalOutputTrue_shouldSucceed()
            throws ApiException, NoApiKeyException, UploadFileException {
        StreamResult result = doStreamCall("qwen3-vl-plus", false, true,
                "qwen3-vl-plus | 不开思考 | 增量输出(true)");
        assertSuccessfulOutput(result);
        assertIncrementalPattern(result, "qwen3-vl-plus 不开思考增量输出");
    }

    /**
     * 场景4: qwen3-vl-plus 不开思考 + 全量输出(false)"
     * 实测: 不开启思考时设置全量输出不会生效
     */
    @Test
    void qwen3VlPlus_noThinking_incrementalOutputFalse_fullOutputNotEffective()
            throws ApiException, NoApiKeyException, UploadFileException {
        StreamResult result = doStreamCall("qwen3-vl-plus", false, false,
                "qwen3-vl-plus | 不开思考 | 全量输出(false) -> 不生效");
        assertSuccessfulOutput(result);
    }

    /**
     * 场景5: qwen-vl-plus 增量输出(true)
     * 预期: 正常增量输出
     */
    @Test
    void qwenVlPlus_incrementalOutputTrue_shouldSucceed()
            throws ApiException, NoApiKeyException, UploadFileException {
        StreamResult result = doStreamCall("qwen-vl-plus", false, true,
                "qwen-vl-plus | 增量输出(true)");
        assertSuccessfulOutput(result);
        assertIncrementalPattern(result, "qwen-vl-plus 增量输出");
    }

    /**
     * 场景6: qwen-vl-plus 全量输出(false)"
     * 实测: 可以成功全量输出，每次回调返回累积完整内容
     */
    @Test
    void qwenVlPlus_incrementalOutputFalse_shouldBeFullOutput()
            throws ApiException, NoApiKeyException, UploadFileException {
        StreamResult result = doStreamCall("qwen-vl-plus", false, false,
                "qwen-vl-plus | 全量输出(false) -> 正常");
        assertSuccessfulOutput(result);
        assertFullOutputPattern(result, "qwen-vl-plus 全量输出");
    }

    // ==================== 公共方法 ====================

    /** 构建多模态用户消息（图片 + 文本） */
    private MultiModalMessage buildUserMessage(String text) {
        return MultiModalMessage.builder()
                .role(Role.USER.getValue())
                .content(Arrays.asList(
                        Collections.singletonMap("image", IMAGE_URL),
                        Collections.singletonMap("text", text)))
                .build();
    }

    /**
     * 执行流式调用并打印结果
     *
     * @param model             模型名称
     * @param enableThinking    是否开启思考
     * @param incrementalOutput true=增量输出 false=全量输出
     * @param label             测试场景标签（用于打印区分）
     * @return 流式调用结果封装
     */
    private StreamResult doStreamCall(String model, boolean enableThinking, boolean incrementalOutput, String label)
            throws ApiException, NoApiKeyException, UploadFileException {
        System.out.println("\n==================================================");
        System.out.println("[" + label + "]");
        System.out.println("  model: " + model);
        System.out.println("  enableThinking: " + enableThinking);
        System.out.println("  incrementalOutput: " + incrementalOutput);
        System.out.println("--------------------------------------------------");

        MultiModalConversation conv = new MultiModalConversation();
        MultiModalMessage userMessage = buildUserMessage("图中描绘的是什么景象？");

        var builder = MultiModalConversationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(Arrays.asList(userMessage))
                .incrementalOutput(incrementalOutput);
        if (enableThinking) {
            builder.enableThinking(true);
        }
        MultiModalConversationParam param = builder.build();

        List<String> fragments = new ArrayList<>();
        AtomicInteger printedLength = new AtomicInteger(0);
        Flowable<MultiModalConversationResult> result = conv.streamCall(param);
        result.blockingForEach(item -> {
            try {
                var content = item.getOutput().getChoices().get(0).getMessage().getContent();
                if (content != null && !content.isEmpty()) {
                    Object textObj = content.get(0).get("text");
                    String text = textObj == null ? "" : textObj.toString();
                    if (!text.isEmpty()) {
                        fragments.add(text);
                        // 打印原始片段，用于直观判断 SDK 实际返回的是增量还是全量
                        String preview = text.length() > 80
                                ? text.substring(0, 80).replace("\n", "\\n") + "..."
                                : text.replace("\n", "\\n");
                        System.out.println("[原始片段 " + fragments.size() + "] len=" + text.length() + " -> " + preview);
                        if (incrementalOutput) {
                            // 增量模式：直接打印每次返回的片段
                            System.out.print(text);
                        } else {
                            // 全量模式：只打印相比上次新增的部分
                            int prev = printedLength.get();
                            if (text.length() > prev) {
                                System.out.print(text.substring(prev));
                                printedLength.set(text.length());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Parse item failed: {}", e.getMessage());
            }
        });
        System.out.println("\n--------------------------------------------------");
        System.out.println("[汇总] 场景: " + label);
        System.out.println("  原始片段总数: " + fragments.size());
        if (!fragments.isEmpty()) {
            System.out.println("  第一个片段长度: " + fragments.get(0).length());
            System.out.println("  最后一个片段长度: " + fragments.get(fragments.size() - 1).length());
            // 判断长度是否单调不减（全量特征）或普遍很小（增量特征）
            boolean monotonicNonDecreasing = true;
            int increases = 0;
            for (int i = 1; i < fragments.size(); i++) {
                if (fragments.get(i).length() < fragments.get(i - 1).length()) {
                    monotonicNonDecreasing = false;
                } else if (fragments.get(i).length() > fragments.get(i - 1).length()) {
                    increases++;
                }
            }
            System.out.println("  长度单调不减: " + monotonicNonDecreasing + " (增长次数=" + increases + ")");
            System.out.println("  判定: " + (monotonicNonDecreasing && increases > fragments.size() / 2 ? "全量返回" : "增量返回"));
        }
        System.out.println("==================================================\n");

        String completeText = incrementalOutput
                ? String.join("", fragments)
                : (fragments.isEmpty() ? "" : fragments.get(fragments.size() - 1));
        return new StreamResult(fragments, completeText, !fragments.isEmpty(), incrementalOutput);
    }

    private void assertSuccessfulOutput(StreamResult result) {
        assertTrue(result.hasContent(), "流式调用应返回非空内容");
        assertFalse(result.completeText().isBlank(), "最终输出文本不应为空");
    }

    private void assertIncrementalPattern(StreamResult result, String message) {
        assertTrue(result.incrementalOutput(), "该断言仅适用于 incrementalOutput=true 的场景");
        String concatenated = String.join("", result.fragments());
        assertEquals(result.completeText(), concatenated,
                message + "：增量片段拼接后应等于完整回复");
    }

    private void assertFullOutputPattern(StreamResult result, String message) {
        assertFalse(result.incrementalOutput(), "该断言仅适用于 incrementalOutput=false 的场景");
        List<String> fragments = result.fragments();
        for (int i = 1; i < fragments.size(); i++) {
            assertTrue(fragments.get(i).length() >= fragments.get(i - 1).length(),
                    message + "：全量输出片段长度应单调不减");
        }
        assertEquals(fragments.get(fragments.size() - 1), result.completeText(),
                message + "：最后一个片段应等于完整回复");
    }

    private record StreamResult(List<String> fragments, String completeText, boolean hasContent, boolean incrementalOutput) {
    }
}
