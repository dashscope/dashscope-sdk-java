// Copyright (c) Alibaba, Inc. and its affiliates.

import com.alibaba.dashscope.app.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;

import java.util.Arrays;
import java.util.Collections;
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
        ApplicationParam param = ApplicationParam.builder()
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
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("API接口说明中, TopP参数改如何传递?")
                .topP(0.2)
                // 开启检索过程信息返回结果
                .hasThoughts(true)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());

        List<ApplicationOutput.Thought> thoughts = result.getOutput().getThoughts();
        if (thoughts != null && !thoughts.isEmpty()) {
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
        if (thoughts != null && !thoughts.isEmpty()) {
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
                .incrementalOutput(true)
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);
        result.blockingForEach(data -> System.out.printf(data.getOutput().getText()));
        System.out.print("\n");
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

    /**
     * Application call with long term memory sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void callWithMemory()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("我想去新疆")
                .memoryId("mem_123")
                .topP(0.2)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s\n", result.getRequestId(), result.getOutput().getText());
    }

    public static void callWithAssistantServing() throws NoApiKeyException, InputRequiredException {
        JsonObject metadataFilter = new JsonObject();
        metadataFilter.addProperty("key", "meta123");

        JsonObject structureFilter = new JsonObject();
        structureFilter.addProperty("key", "structured123");

        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("我想去新疆")
                .images(Collections.singletonList("img_123"))
                .ragOptions(RagOptions.builder()
                        .tags(Collections.singletonList("tag_123"))
                        .pipelineIds(Collections.singletonList("pipeline_123"))
                        .fileIds(Collections.singletonList("files_123"))
                        .metadataFilter(metadataFilter)
                        .structuredFilter(structureFilter)
                        .build())
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s\n", result.getRequestId(), result.getOutput().getText());

    }

    public static void ragCallWithDocReference()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("ChatDev的亮点是什么？")
                .topP(0.2)
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.printf("requestId: %s, text: %s, finishReason: %s\n",
                result.getRequestId(), result.getOutput().getText(), result.getOutput().getFinishReason());

        if (result.getOutput().getDocReferences() != null && !result.getOutput().getDocReferences().isEmpty()) {
            for (ApplicationOutput.DocReference docReference : result.getOutput().getDocReferences()) {
                System.out.println(docReference.toString());
            }
        }
    }

    public static void callWithMoreParameters() throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
//                .enablePremium(true)
                .enableSystemTime(true)
                .enableWebSearch(true)
                .mcpServers(Arrays.asList("amap-maps", "Meitu"))
                .dialogRound(2)
                .modelId("qwen-plus")
                .prompt("从杭州西湖到杭州东站，打车多少钱?")
                .topP(0.8)
                .incrementalOutput(true)
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);
        result.blockingForEach(data -> System.out.printf("result: %s%n", data));
//        result.blockingForEach(data -> System.out.printf(data.getOutput().getText()));
        System.out.print("\n");
    }

    public static void callWithThinking() throws NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("1.1和0.9哪个大?")
                .enableThinking(true)
                .hasThoughts(true)
                .incrementalOutput(true)
                .build();

        Application application = new Application();
        Flowable<ApplicationResult> result = application.streamCall(param);
        result.blockingForEach(data -> System.out.printf("result: %s%n", data));
        System.out.print("\n");
    }

    /**
     * Call with file list sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws ApiException           The request failed, possibly
     *                                due to a network or data error.
     * @throws InputRequiredException Missing inputs.
     */
    public static void callWithFileList()
            throws ApiException, NoApiKeyException, InputRequiredException {
        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("总结文件内容")
                .files(Collections.singletonList(
                    "https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3"))
                .build();

        Application application = new Application();
        ApplicationResult result = application.call(param);

        System.out.println(JsonUtils.toJson(result));
    }

    /**
     * Stream call with CIP (Content Integrity Protection) parameters
     * sample
     *
     * @throws NoApiKeyException      Can not find api key
     * @throws InputRequiredException Missing inputs.
     */
    public static void streamCallWithCIP()
            throws NoApiKeyException, InputRequiredException {
        // Build CIP service codes for content security check
        CipServiceCodes.Text textCheck =
            CipServiceCodes.Text.builder()
                .input("query_security_check")
                .output("response_security_check")
                .build();

        CipServiceCodes.Image imageCheck =
            CipServiceCodes.Image.builder()
                .input("img_query_security_check")
                .build();

        CipServiceCodes cipServiceCodes = CipServiceCodes.builder()
            .text(textCheck)
            .image(imageCheck)
            .build();

        ApplicationParam param = ApplicationParam.builder()
                .appId(APP_ID)
                .prompt("图片里是什么内容")
                .images(Collections.singletonList("https://yutai007.oss-cn-beijing.aliyuncs.com/documentsForTest/image/%E7%BE%BD%E7%BB%92%E6%9C%8D-wgggc.jpg"))
                .cipServiceCodes(cipServiceCodes)
//                .incrementalOutput(true)
                .build();

        Application application = new Application();
//        Flowable<ApplicationResult> result = application.streamCall(param);
//        result.blockingForEach(data ->
//            System.out.println(JsonUtils.toJson(data)));
        ApplicationResult result = application.call(param);
        System.out.println(JsonUtils.toJson(result));
    }


    public static void main(String[] args) {
        try {
//        ragCall();
//        ragCallWithTags();
//        flowCall();
//        callWithSession();
//        streamCall();
//        callWithWorkspace();
//            callWithMemory();
//            callWithAssistantServing();
//            ragCallWithDocReference();
//            callWithMoreParameters();
//            callWithThinking();
//            callWithFileList();
//            streamCallWithFileList();
            streamCallWithCIP();
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            System.out.printf("Exception: %s", e.getMessage());
        }
        System.exit(0);
    }
}
