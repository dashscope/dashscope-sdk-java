import com.alibaba.dashscope.aigc.codegeneration.CodeGeneration;
import com.alibaba.dashscope.aigc.codegeneration.CodeGenerationParam;
import com.alibaba.dashscope.aigc.codegeneration.CodeGenerationResult;
import com.alibaba.dashscope.aigc.codegeneration.models.AttachmentRoleMessageParam;
import com.alibaba.dashscope.aigc.codegeneration.models.MessageParamBase;
import com.alibaba.dashscope.aigc.codegeneration.models.UserRoleMessageParam;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class CodeGenerationQuickStart {

    public static final String MODEL = CodeGeneration.Models.TONGYI_LINGMA_V1;

    public static void testCustomSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CUSTOM)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testCustomSample：" + JsonUtils.toJson(result));
    }

    public static void testNl2codeSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("计算给定路径下所有文件的总大小"));
        String meta = "{\"language\": \"java\"}";
        messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.NL2CODE)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testNl2codeSample：" + JsonUtils.toJson(result));
    }

    public static void testCode2commentSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("1. 生成中文注释\\n2. 仅生成代码部分，不需要额外解释函数功能\\n"));
        String meta = "{\"code\": \"\\t\\t@Override\\n\\t\\tpublic  CancelExportTaskResponse  cancelExportTask(\\n\\t\\t\\t\\tCancelExportTask  cancelExportTask)  {\\n\\t\\t\\tAmazonEC2SkeletonInterface  ec2Service  =  ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);\\n\\t\\t\\treturn  ec2Service.cancelExportTask(cancelExportTask);\\n\\t\\t}\", \"language\": \"java\"}";
        messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CODE2COMMENT)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testCode2commentSample：" + JsonUtils.toJson(result));
    }

    public static void testCode2explainSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("要求不低于200字"));
        String meta = "{\"code\": \"@Override\\n                                public  int  getHeaderCacheSize()\\n                                {\\n                                        return  0;\\n                                }\\n\\n\", \"language\": \"java\"}";
        messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CODE2EXPLAIN)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testCode2explainSample：" + JsonUtils.toJson(result));
    }

    public static void testCommit2msgSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        String meta = "{\"diff_list\": [{\"diff\": \"--- src/com/siondream/core/PlatformResolver.java\\n+++ src/com/siondream/core/PlatformResolver.java\\n@@ -1,11 +1,8 @@\\npackage com.siondream.core;\\n-\\n-import com.badlogic.gdx.files.FileHandle;\\n\\npublic interface PlatformResolver {\\npublic void openURL(String url);\\npublic void rateApp();\\npublic void sendFeedback();\\n-\\tpublic FileHandle[] listFolder(String path);\\n}\\n\", \"old_file_path\": \"src/com/siondream/core/PlatformResolver.java\", \"new_file_path\": \"src/com/siondream/core/PlatformResolver.java\"}]}";
        messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.COMMIT2MSG)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testCommit2msgSample：" + JsonUtils.toJson(result));
    }

    public static void testUnittestSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        String meta = "{\"code\": \"public static <T> TimestampMap<T> parseTimestampMap(Class<T> typeClass, String input, DateTimeZone timeZone) throws IllegalArgumentException {\\n        if (typeClass == null) {\\n            throw new IllegalArgumentException(\\\"typeClass required\\\");\\n        }\\n\\n        if (input == null) {\\n            return null;\\n        }\\n\\n        TimestampMap result;\\n\\n        typeClass = AttributeUtils.getStandardizedType(typeClass);\\n        if (typeClass.equals(String.class)) {\\n            result = new TimestampStringMap();\\n        } else if (typeClass.equals(Byte.class)) {\\n            result = new TimestampByteMap();\\n        } else if (typeClass.equals(Short.class)) {\\n            result = new TimestampShortMap();\\n        } else if (typeClass.equals(Integer.class)) {\\n            result = new TimestampIntegerMap();\\n        } else if (typeClass.equals(Long.class)) {\\n            result = new TimestampLongMap();\\n        } else if (typeClass.equals(Float.class)) {\\n            result = new TimestampFloatMap();\\n        } else if (typeClass.equals(Double.class)) {\\n            result = new TimestampDoubleMap();\\n        } else if (typeClass.equals(Boolean.class)) {\\n            result = new TimestampBooleanMap();\\n        } else if (typeClass.equals(Character.class)) {\\n            result = new TimestampCharMap();\\n        } else {\\n            throw new IllegalArgumentException(\\\"Unsupported type \\\" + typeClass.getClass().getCanonicalName());\\n        }\\n\\n        if (input.equalsIgnoreCase(EMPTY_VALUE)) {\\n            return result;\\n        }\\n\\n        StringReader reader = new StringReader(input + ' ');// Add 1 space so\\n                                                            // reader.skip\\n                                                            // function always\\n                                                            // works when\\n                                                            // necessary (end of\\n                                                            // string not\\n                                                            // reached).\\n\\n        try {\\n            int r;\\n            char c;\\n            while ((r = reader.read()) != -1) {\\n                c = (char) r;\\n                switch (c) {\\n                    case LEFT_BOUND_SQUARE_BRACKET:\\n                    case LEFT_BOUND_BRACKET:\\n                        parseTimestampAndValue(typeClass, reader, result, timeZone);\\n                        break;\\n                    default:\\n                        // Ignore other chars outside of bounds\\n                }\\n            }\\n        } catch (IOException ex) {\\n            throw new RuntimeException(\\\"Unexpected expection while parsing timestamps\\\", ex);\\n        }\\n\\n        return result;\\n    }\", \"language\": \"java\"}";
        messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.UNIT_TEST)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testUnittestSample：" + JsonUtils.toJson(result));
    }

    public static void testCodeqaSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("I'm writing a small web server in Python, using BaseHTTPServer and a custom subclass of BaseHTTPServer.BaseHTTPRequestHandler. Is it possible to make this listen on more than one port?\\nWhat I'm doing now:\\nclass MyRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):\\n  def doGET\\n  [...]\\n\\nclass ThreadingHTTPServer(ThreadingMixIn, HTTPServer): \\n    pass\\n\\nserver = ThreadingHTTPServer(('localhost', 80), MyRequestHandler)\\nserver.serve_forever()"));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CODE_QA)
                        .message(messageParams)
                        .build();

        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testCodeqaSample：" + JsonUtils.toJson(result));
    }

    public static void testNl2sqlSample() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("小明的总分数是多少"));
        String meta = "{\"synonym_infos\": {\"学生姓名\": \"姓名|名字|名称\", \"学生分数\": \"分数|得分\"}, \"recall_infos\": [{\"content\": \"student_score.id='小明'\", \"score\": \"0.83\"}], \"schema_infos\": [{\"table_id\": \"student_score\", \"table_desc\": \"学生分数表\", \"columns\": [{\"col_name\": \"id\", \"col_caption\": \"学生id\", \"col_desc\": \"例值为:1,2,3\", \"col_type\": \"string\"}, {\"col_name\": \"name\", \"col_caption\": \"学生姓名\", \"col_desc\": \"例值为:张三,李四,小明\", \"col_type\": \"string\"}, {\"col_name\": \"score\", \"col_caption\": \"学生分数\", \"col_desc\": \"例值为:98,100,66\", \"col_type\": \"string\"}]}]}";
        messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.NL2SQL)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        CodeGenerationResult result = generation.call(param);
        System.out.println("testNl2sqlSample：" + JsonUtils.toJson(result));
    }

    public static void testCustomSampleWithCallBack() throws ApiException, NoApiKeyException, InterruptedException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CUSTOM)
                        .message(messageParams)
                        .build();

        CodeGeneration generation = new CodeGeneration();
        Semaphore semaphore = new Semaphore(0);
        List<CodeGenerationResult> results = new ArrayList<>();
        generation.call(
                param,
                new ResultCallback<CodeGenerationResult>() {
                    @Override
                    public void onEvent(CodeGenerationResult msg) {
                        results.add(msg);
                    }

                    @Override
                    public void onComplete() {
                        semaphore.release();
                    }

                    @Override
                    public void onError(Exception e) {
                        semaphore.release();
                    }
                });
        semaphore.acquire();
        System.out.println("testCustomSampleWithCallBack：" + JsonUtils.toJson(results));
    }

    public static void testCustomSampleWithStream() throws ApiException, NoApiKeyException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CUSTOM)
                                .message(messageParams)
                                .build();
        CodeGeneration generation = new CodeGeneration();
        Flowable<CodeGenerationResult> flowable = generation.streamCall(param);
        flowable.blockingForEach(result -> {
            System.out.println("testCustomSampleWithStream forEach：" + JsonUtils.toJson(result));
        });
    }

    public static void testCustomSampleWithStreamCallBack() throws ApiException, NoApiKeyException, InterruptedException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CUSTOM)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration();
        Semaphore semaphore = new Semaphore(0);
        List<CodeGenerationResult> results = new ArrayList<>();
        generation.streamCall(
                param,
                new ResultCallback<CodeGenerationResult>() {
                    @Override
                    public void onEvent(CodeGenerationResult msg) {
                        results.add(msg);
                    }

                    @Override
                    public void onComplete() {
                        semaphore.release();
                    }

                    @Override
                    public void onError(Exception e) {
                        semaphore.release();
                    }
                });
        semaphore.acquire();
        System.out.println("testCustomSampleWithStreamCallBack：" + JsonUtils.toJson(results));
    }

    public static void testCustomSampleWithWebSocket() throws ApiException, NoApiKeyException, InterruptedException, InputRequiredException {
        List<MessageParamBase> messageParams = new ArrayList<>();
        messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
        CodeGenerationParam param =
                CodeGenerationParam.builder()
                        .model(MODEL)
                        .scene(CodeGeneration.Scenes.CUSTOM)
                        .message(messageParams)
                        .build();
        CodeGeneration generation = new CodeGeneration(Protocol.WEBSOCKET.getValue());
        CodeGenerationResult result = generation.call(param);
        System.out.println(result);
        System.exit(0);
    }

    public static void main(String[] args) {
        try {
            testCustomSample();
            testNl2codeSample();
            testCode2commentSample();
            testCode2explainSample();
            testCommit2msgSample();
            testUnittestSample();
            testCodeqaSample();
            testNl2sqlSample();
            testCustomSampleWithCallBack();
            testCustomSampleWithStream();
            testCustomSampleWithStreamCallBack();
            testCustomSampleWithWebSocket();
        } catch (NoApiKeyException | InputRequiredException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
