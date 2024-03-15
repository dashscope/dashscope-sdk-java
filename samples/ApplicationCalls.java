// Copyright (c) Alibaba, Inc. and its affiliates.
import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.List;

/**
 * Title App Completion call samples.<br>
 * Description App Completion call samples.<br>
 * Created at 2024-02-26 09:50
 *
 * @since jdk8
 */

public class ApplicationCalls {
    private static final String WORKSPACE;
    private static final String APP_ID;

    static {
        WORKSPACE = System.getenv("WORKSPACE_ID");
        APP_ID = System.getenv("APP_ID");
    }

    /**
     * Rag application call sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void ragCall()
            throws ApiException, NoApiKeyException, InputRequiredException {
        RagApplicationParam param = RagApplicationParam.builder()
                .appId(APP_ID)
                .prompt("API接口说明中, TopP参数改如何传递?")
                .topP(0.2)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());

        if (result.getUsage() != null && result.getUsage().getModels() != null) {
            for (ApplicationUsage.ModelUsage usage : result.getUsage().getModels()) {
                System.out.printf("modelId: %s, inputTokens: %d, outputTokens: %d\n",
                        usage.getModelId(), usage.getInputTokens(), usage.getOutputTokens());
            }
        }
    }

    /**
     * Rag application call with tags sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void ragCallWithTags()
            throws ApiException, NoApiKeyException, InputRequiredException {
        RagApplicationParam param = RagApplicationParam.builder()
                .appId(APP_ID)
                .prompt("API接口说明中, TopP参数改如何传递?")
                .topP(0.2)
                // 指定标签范围进行检索
                .docTagCodes(Arrays.asList("471d*******3427", "471d*******3428"))
                // 开启检索过程信息返回结果
                .hasThoughts(true)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());

        List<ApplicationOutput.Thought> thoughts = result.getOutput().getThoughts();
        if (thoughts != null && thoughts.size() > 0) {
            for (ApplicationOutput.Thought thought : thoughts) {
                System.out.printf("thought: %s\n", thought);
            }
        }
    }

    /**
     * Plugin and flow application call sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void flowCall()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("杭州的天气怎么样")
                .topP(0.2)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());
    }

    /**
     * Plugin and flow application call with biz params sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void flowCallWithParam()
            throws ApiException, NoApiKeyException, InputRequiredException {

        //查询今天的天气
        String bizParams = "{\"date\": \"今天\"}";

        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("杭州的天气怎么样")
                // 传递业务参数
                .bizParams(JsonUtils.parse(bizParams))
                // 开启插件调用过程返回结果
                .hasThoughts(true)
                .topP(0.2)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());

        List<ApplicationOutput.Thought> thoughts = result.getOutput().getThoughts();
        if (thoughts != null && thoughts.size() > 0) {
            for (ApplicationOutput.Thought thought : thoughts) {
                System.out.printf("thought: %s\n", thought);
            }
        }
    }

    /**
     * Call with multiple rounds session sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void callWithSession()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("我想去新疆")
                .topP(0.2)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        param.setSessionId(result.getOutput().getSessionId());
        param.setPrompt("那边有什么旅游景点或者美食?");
        result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());
    }

    /**
     * Call with stream response(Http SSE) sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws InputRequiredException Missing inputs.
     */
    public static void streamCall() throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("如何做土豆炖猪脚?")
                .topP(0.8)
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);
        result.blockingForEach(data -> {
            System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                    data.getRequestId(), data.getOutput().getText(), data.getOutput().getFinishReason());

        });
    }

    /**
     * Call with workspace sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws InputRequiredException Missing inputs.
     */
    public static void callWithWorkspace() throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .workspace(WORKSPACE)
                .appId(APP_ID)
                .prompt("如何做土豆炖猪脚?")
                .topP(0.8)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());
    }


    public static void main(String[] args) {
        try {
//            ragCall();
//        ragCallWithTags();
//        flowCall();
//        callWithSession();
//        streamCall();
        callWithWorkspace();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.printf("Exception: %s", e.getMessage());
        }
        System.exit(0);
    }
}
