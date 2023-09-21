package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

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
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.reactivex.Flowable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestCodeGeneration {

  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  private static final MediaType MEDIA_TYPE_EVENT_STREAM = MediaType.parse("text/event-stream");

  public static final String MODEL = CodeGeneration.Models.TONGYI_LINGMA_V1;

  String customExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"custom\", \"message\": [{\"role\": \"user\", \"content\": \"根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。\"}]}}";
  String nl2codeExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"nl2code\", \"message\": [{\"role\": \"user\", \"content\": \"计算给定路径下所有文件的总大小\"}, {\"role\": \"attachment\", \"meta\": {\"language\": \"java\"}}]}}";
  String code2commentExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"code2comment\", \"message\": [{\"role\": \"user\", \"content\": \"1. 生成中文注释\\n2. 仅生成代码部分，不需要额外解释函数功能\\n\"}, {\"role\": \"attachment\", \"meta\": {\"code\": \"\\t\\t@Override\\n\\t\\tpublic  CancelExportTaskResponse  cancelExportTask(\\n\\t\\t\\t\\tCancelExportTask  cancelExportTask)  {\\n\\t\\t\\tAmazonEC2SkeletonInterface  ec2Service  =  ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);\\n\\t\\t\\treturn  ec2Service.cancelExportTask(cancelExportTask);\\n\\t\\t}\", \"language\": \"java\"}}]}}";
  String code2explainExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"code2explain\", \"message\": [{\"role\": \"user\", \"content\": \"要求不低于200字\"}, {\"role\": \"attachment\", \"meta\": {\"code\": \"@Override\\n                                public  int  getHeaderCacheSize()\\n                                {\\n                                        return  0;\\n                                }\\n\\n\", \"language\": \"java\"}}]}}";
  String commit2msgExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"commit2msg\", \"message\": [{\"role\": \"attachment\", \"meta\": {\"diff_list\": [{\"diff\": \"--- src/com/siondream/core/PlatformResolver.java\\n+++ src/com/siondream/core/PlatformResolver.java\\n@@ -1,11 +1,8 @@\\npackage com.siondream.core;\\n-\\n-import com.badlogic.gdx.files.FileHandle;\\n\\npublic interface PlatformResolver {\\npublic void openURL(String url);\\npublic void rateApp();\\npublic void sendFeedback();\\n-\\tpublic FileHandle[] listFolder(String path);\\n}\\n\", \"old_file_path\": \"src/com/siondream/core/PlatformResolver.java\", \"new_file_path\": \"src/com/siondream/core/PlatformResolver.java\"}]}}]}}";
  String unittestExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"unittest\", \"message\": [{\"role\": \"attachment\", \"meta\": {\"code\": \"public static <T> TimestampMap<T> parseTimestampMap(Class<T> typeClass, String input, DateTimeZone timeZone) throws IllegalArgumentException {\\n        if (typeClass == null) {\\n            throw new IllegalArgumentException(\\\"typeClass required\\\");\\n        }\\n\\n        if (input == null) {\\n            return null;\\n        }\\n\\n        TimestampMap result;\\n\\n        typeClass = AttributeUtils.getStandardizedType(typeClass);\\n        if (typeClass.equals(String.class)) {\\n            result = new TimestampStringMap();\\n        } else if (typeClass.equals(Byte.class)) {\\n            result = new TimestampByteMap();\\n        } else if (typeClass.equals(Short.class)) {\\n            result = new TimestampShortMap();\\n        } else if (typeClass.equals(Integer.class)) {\\n            result = new TimestampIntegerMap();\\n        } else if (typeClass.equals(Long.class)) {\\n            result = new TimestampLongMap();\\n        } else if (typeClass.equals(Float.class)) {\\n            result = new TimestampFloatMap();\\n        } else if (typeClass.equals(Double.class)) {\\n            result = new TimestampDoubleMap();\\n        } else if (typeClass.equals(Boolean.class)) {\\n            result = new TimestampBooleanMap();\\n        } else if (typeClass.equals(Character.class)) {\\n            result = new TimestampCharMap();\\n        } else {\\n            throw new IllegalArgumentException(\\\"Unsupported type \\\" + typeClass.getClass().getCanonicalName());\\n        }\\n\\n        if (input.equalsIgnoreCase(EMPTY_VALUE)) {\\n            return result;\\n        }\\n\\n        StringReader reader = new StringReader(input + ' ');// Add 1 space so\\n                                                            // reader.skip\\n                                                            // function always\\n                                                            // works when\\n                                                            // necessary (end of\\n                                                            // string not\\n                                                            // reached).\\n\\n        try {\\n            int r;\\n            char c;\\n            while ((r = reader.read()) != -1) {\\n                c = (char) r;\\n                switch (c) {\\n                    case LEFT_BOUND_SQUARE_BRACKET:\\n                    case LEFT_BOUND_BRACKET:\\n                        parseTimestampAndValue(typeClass, reader, result, timeZone);\\n                        break;\\n                    default:\\n                        // Ignore other chars outside of bounds\\n                }\\n            }\\n        } catch (IOException ex) {\\n            throw new RuntimeException(\\\"Unexpected expection while parsing timestamps\\\", ex);\\n        }\\n\\n        return result;\\n    }\", \"language\": \"java\"}}]}}";
  String codeqaExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"codeqa\", \"message\": [{\"role\": \"user\", \"content\": \"I'm writing a small web server in Python, using BaseHTTPServer and a custom subclass of BaseHTTPServer.BaseHTTPRequestHandler. Is it possible to make this listen on more than one port?\\nWhat I'm doing now:\\nclass MyRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):\\n  def doGET\\n  [...]\\n\\nclass ThreadingHTTPServer(ThreadingMixIn, HTTPServer): \\n    pass\\n\\nserver = ThreadingHTTPServer(('localhost', 80), MyRequestHandler)\\nserver.serve_forever()\"}]}}";
  String nl2sqlExpectTextBody =
      "{\"model\": \"tongyi-lingma-v1\", \"parameters\": {\"n\": 1}, \"input\": {\"scene\": \"nl2sql\", \"message\": [{\"role\": \"user\", \"content\": \"小明的总分数是多少\"}, {\"role\": \"attachment\", \"meta\": {\"synonym_infos\": {\"学生姓名\": \"姓名|名字|名称\", \"学生分数\": \"分数|得分\"}, \"recall_infos\": [{\"content\": \"student_score.id='小明'\", \"score\": \"0.83\"}], \"schema_infos\": [{\"table_id\": \"student_score\", \"table_desc\": \"学生分数表\", \"columns\": [{\"col_name\": \"id\", \"col_caption\": \"学生id\", \"col_desc\": \"例值为:1,2,3\", \"col_type\": \"string\"}, {\"col_name\": \"name\", \"col_caption\": \"学生姓名\", \"col_desc\": \"例值为:张三,李四,小明\", \"col_type\": \"string\"}, {\"col_name\": \"score\", \"col_caption\": \"学生分数\", \"col_desc\": \"例值为:98,100,66\", \"col_type\": \"string\"}]}]}}]}}";

  MockWebServer server;

  @BeforeEach
  public void before() {
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  private void checkResult(
      CodeGenerationResult result, RecordedRequest request, String expectBody, TestResponse rsp) {
    assertEquals(result.getRequestId(), rsp.getRequestId());
    assertEquals(JsonUtils.toJson(result.getUsage()), rsp.getUsage().toString());
    assertEquals(
        result.getOutput().getChoices().get(0).getFinishReason(),
        rsp.getOutput()
            .getAsJsonArray("choices")
            .get(0)
            .getAsJsonObject()
            .get("finish_reason")
            .getAsString());
    assertEquals(
        result.getOutput().getChoices().get(0).getFrameTimestamp(),
        new Double(
            rsp.getOutput()
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .get("frame_timestamp")
                .getAsDouble()));
    assertEquals(
        result.getOutput().getChoices().get(0).getIndex(),
        new Integer(
            rsp.getOutput()
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .get("index")
                .getAsInt()));
    assertEquals(
        result.getOutput().getChoices().get(0).getContent(),
        rsp.getOutput()
            .getAsJsonArray("choices")
            .get(0)
            .getAsJsonObject()
            .get("content")
            .getAsString());
    assertEquals(
        result.getOutput().getChoices().get(0).getFrameId(),
        new Integer(
            rsp.getOutput()
                .getAsJsonArray("choices")
                .get(0)
                .getAsJsonObject()
                .get("frame_id")
                .getAsInt()));
    String body = request.getBody().readUtf8();
    body = body.replace("\\\\n", "\\n").replace("\\\\t", "\\t").replace("\\\\\"", "\"");
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/aigc/code-generation/generation");
    assertEquals(JsonUtils.parse(body), JsonUtils.parse(expectBody));
  }

  @Test
  public void testCustomSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694702346.730724);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "以下是生成Python函数的代码：\\n\\n```python\\ndef file_size(path):\\n    total_size = 0\\n    for root, dirs, files in os.walk(path):\\n        for file in files:\\n            full_path = os.path.join(root, file)\\n            total_size += os.path.getsize(full_path)\\n    return total_size\\n```\\n\\n函数名为`file_size`，输入参数是给定路径`path`。函数通过递归遍历给定路径下的所有文件，使用`os.walk`函数遍历根目录及其子目录下的文件，计算每个文件的大小并累加到总大小上。最后，返回总大小作为函数的返回值。");
    choice.addProperty("frame_id", 25);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 46);
    usage.addProperty("output_tokens", 198);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("bf321b27-a3ff-9674-a70e-be5f40a435e4")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CUSTOM)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, customExpectTextBody, rsp);
  }

  @Test
  public void testNl2codeSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694692088.1848974);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "\"```java\\n/**\\n * 计算给定路径下所有文件的总大小\\n * @param path 路径\\n * @return 总大小，单位为字节\\n */\\npublic static long getTotalFileSize(String path) {\\n    long size = 0;\\n    try {\\n        File file = new File(path);\\n        File[] files = file.listFiles();\\n        for (File f : files) {\\n            if (f.isFile()) {\\n                size += f.length();\\n            }\\n        }\\n    } catch (Exception e) {\\n        e.printStackTrace();\\n    }\\n    return size;\\n}\\n```\\n\\n使用方式:\\n```java\\nlong size = getTotalFileSize(\\\"/home/user/Documents/\\\");\\nSystem.out.println(\\\"总大小：\\\" + size + \\\"字节\\\");\\n```\\n\\n示例输出:\\n```\\n总大小：37144952字节\\n```");
    choice.addProperty("frame_id", 29);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 39);
    usage.addProperty("output_tokens", 229);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("59bbbea3-29a7-94d6-8c39-e4d6e465f640")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
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
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, nl2codeExpectTextBody, rsp);
  }

  @Test
  public void testCode2commentSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694692326.983717);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "```java\\n/**\\n * 取消导出任务的回调函数\\n *\\n * @param cancelExportTask 取消导出任务的请求对象\\n * @return 取消导出任务的响应对象\\n */\\n@Override\\npublic CancelExportTaskResponse cancelExportTask(CancelExportTask cancelExportTask) {\\n\\tAmazonEC2SkeletonInterface ec2Service = ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);\\n\\treturn ec2Service.cancelExportTask(cancelExportTask);\\n}\\n```");
    choice.addProperty("frame_id", 17);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 141);
    usage.addProperty("output_tokens", 133);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("b5e55877-bfa3-9863-88d8-09a72124cf8a")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("1. 生成中文注释\\n2. 仅生成代码部分，不需要额外解释函数功能\\n"));
    String meta =
        "{\"code\": \"\\t\\t@Override\\n\\t\\tpublic  CancelExportTaskResponse  cancelExportTask(\\n\\t\\t\\t\\tCancelExportTask  cancelExportTask)  {\\n\\t\\t\\tAmazonEC2SkeletonInterface  ec2Service  =  ServiceProvider.getInstance().getServiceImpl(AmazonEC2SkeletonInterface.class);\\n\\t\\t\\treturn  ec2Service.cancelExportTask(cancelExportTask);\\n\\t\\t}\", \"language\": \"java\"}";
    messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CODE2COMMENT)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, code2commentExpectTextBody, rsp);
  }

  @Test
  public void testCode2explainSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694697070.7664366);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "这个Java函数是一个覆盖了另一个方法的函数，名为`getHeaderCacheSize()`。这个方法是从另一个已覆盖的方法继承过来的。在`@Override`声明中，可以确定这个函数覆盖了一个其他的函数。这个函数的返回类型是`int`。\\n\\n函数内容是：返回0。这个值意味着在`getHeaderCacheSize()`方法中，不会进行任何处理或更新。因此，返回的`0`值应该是没有被处理或更新的值。\\n\\n总的来说，这个函数的作用可能是为了让另一个方法返回一个预设的值。但是由于`@Override`的提示，我们无法确定它的真正目的，需要进一步查看代码才能得到更多的信息。");
    choice.addProperty("frame_id", 30);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 55);
    usage.addProperty("output_tokens", 235);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("089e525f-d28f-9e08-baa2-01dde87c90a7")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("要求不低于200字"));
    String meta =
        "{\"code\": \"@Override\\n                                public  int  getHeaderCacheSize()\\n                                {\\n                                        return  0;\\n                                }\\n\\n\", \"language\": \"java\"}";
    messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CODE2EXPLAIN)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, code2explainExpectTextBody, rsp);
  }

  @Test
  public void testCommit2msgSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694697276.4451804);
    choice.addProperty("index", 0);
    choice.addProperty("content", "Remove old listFolder method");
    choice.addProperty("frame_id", 1);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 197);
    usage.addProperty("output_tokens", 5);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("8f400a4e-6448-94ab-89bf-a97b1a7e6fe6")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    String meta =
        "{\"diff_list\": [{\"diff\": \"--- src/com/siondream/core/PlatformResolver.java\\n+++ src/com/siondream/core/PlatformResolver.java\\n@@ -1,11 +1,8 @@\\npackage com.siondream.core;\\n-\\n-import com.badlogic.gdx.files.FileHandle;\\n\\npublic interface PlatformResolver {\\npublic void openURL(String url);\\npublic void rateApp();\\npublic void sendFeedback();\\n-\\tpublic FileHandle[] listFolder(String path);\\n}\\n\", \"old_file_path\": \"src/com/siondream/core/PlatformResolver.java\", \"new_file_path\": \"src/com/siondream/core/PlatformResolver.java\"}]}";
    messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.COMMIT2MSG)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, commit2msgExpectTextBody, rsp);
  }

  @Test
  public void testUnittestSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694697446.0802872);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "这个函数用于解析时间戳映射表的输入字符串并返回该映射表的实例。函数有两个必选参数：typeClass - 用于标识数据类型的泛型；input - 输入的时间戳映射表字符串。如果typeClass为null，将抛出IllegalArgumentException异常；如果input为null，则返回null。函数内部首先检查输入的字符串是否等于\\\"空字符串\\\"，如果是，则直接返回null；如果不是，则创建TimestampMap的实例，并使用input字符串创建字符串Reader对象。然后使用读取器逐个字符解析时间戳字符串，并在解析完成后返回相应的TimestampMap对象。函数的行为取决于传入的时间戳字符串类型。");
    choice.addProperty("frame_id", 29);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 659);
    usage.addProperty("output_tokens", 227);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("6ec31e35-f355-9289-a18d-103abc36dece")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    String meta =
        "{\"code\": \"public static <T> TimestampMap<T> parseTimestampMap(Class<T> typeClass, String input, DateTimeZone timeZone) throws IllegalArgumentException {\\n        if (typeClass == null) {\\n            throw new IllegalArgumentException(\\\"typeClass required\\\");\\n        }\\n\\n        if (input == null) {\\n            return null;\\n        }\\n\\n        TimestampMap result;\\n\\n        typeClass = AttributeUtils.getStandardizedType(typeClass);\\n        if (typeClass.equals(String.class)) {\\n            result = new TimestampStringMap();\\n        } else if (typeClass.equals(Byte.class)) {\\n            result = new TimestampByteMap();\\n        } else if (typeClass.equals(Short.class)) {\\n            result = new TimestampShortMap();\\n        } else if (typeClass.equals(Integer.class)) {\\n            result = new TimestampIntegerMap();\\n        } else if (typeClass.equals(Long.class)) {\\n            result = new TimestampLongMap();\\n        } else if (typeClass.equals(Float.class)) {\\n            result = new TimestampFloatMap();\\n        } else if (typeClass.equals(Double.class)) {\\n            result = new TimestampDoubleMap();\\n        } else if (typeClass.equals(Boolean.class)) {\\n            result = new TimestampBooleanMap();\\n        } else if (typeClass.equals(Character.class)) {\\n            result = new TimestampCharMap();\\n        } else {\\n            throw new IllegalArgumentException(\\\"Unsupported type \\\" + typeClass.getClass().getCanonicalName());\\n        }\\n\\n        if (input.equalsIgnoreCase(EMPTY_VALUE)) {\\n            return result;\\n        }\\n\\n        StringReader reader = new StringReader(input + ' ');// Add 1 space so\\n                                                            // reader.skip\\n                                                            // function always\\n                                                            // works when\\n                                                            // necessary (end of\\n                                                            // string not\\n                                                            // reached).\\n\\n        try {\\n            int r;\\n            char c;\\n            while ((r = reader.read()) != -1) {\\n                c = (char) r;\\n                switch (c) {\\n                    case LEFT_BOUND_SQUARE_BRACKET:\\n                    case LEFT_BOUND_BRACKET:\\n                        parseTimestampAndValue(typeClass, reader, result, timeZone);\\n                        break;\\n                    default:\\n                        // Ignore other chars outside of bounds\\n                }\\n            }\\n        } catch (IOException ex) {\\n            throw new RuntimeException(\\\"Unexpected expection while parsing timestamps\\\", ex);\\n        }\\n\\n        return result;\\n    }\", \"language\": \"java\"}";
    messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.UNIT_TEST)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, unittestExpectTextBody, rsp);
  }

  @Test
  public void testCodeqaSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694700989.0357094);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "Yes, this is possible:\\nclass MyRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):\\n  [...]\\n\\n  def doGET(self):\\n    # some stuff\\n    if \\\"X-Port\\\" in self.headers:\\n      # change the port in this request\\n      self.server_port = int(self.headers[\\\"X-Port\\\"])\\n      print(\\\"Changed port: %s\\\" % self.server_port)\\n    [...]\\n\\nclass ThreadingHTTPServer(ThreadingMixIn, HTTPServer): \\n    pass\\n\\nserver = ThreadingHTTPServer(('localhost', self.server_port), MyRequestHandler)\\nserver.serve_forever()");
    choice.addProperty("frame_id", 19);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 127);
    usage.addProperty("output_tokens", 150);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("e09386b7-5171-96b0-9c6f-7128507e14e6")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(
        new UserRoleMessageParam(
            "I'm writing a small web server in Python, using BaseHTTPServer and a custom subclass of BaseHTTPServer.BaseHTTPRequestHandler. Is it possible to make this listen on more than one port?\\nWhat I'm doing now:\\nclass MyRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):\\n  def doGET\\n  [...]\\n\\nclass ThreadingHTTPServer(ThreadingMixIn, HTTPServer): \\n    pass\\n\\nserver = ThreadingHTTPServer(('localhost', 80), MyRequestHandler)\\nserver.serve_forever()"));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CODE_QA)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, codeqaExpectTextBody, rsp);
  }

  @Test
  public void testNl2sqlSample()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694701323.4553578);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content", "SELECT SUM(score) as '小明的总分数' FROM student_score WHERE name = '小明';");
    choice.addProperty("frame_id", 3);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 420);
    usage.addProperty("output_tokens", 25);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("e61a35b7-db6f-90c2-8677-9620ffea63b6")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("小明的总分数是多少"));
    String meta =
        "{\"synonym_infos\": {\"学生姓名\": \"姓名|名字|名称\", \"学生分数\": \"分数|得分\"}, \"recall_infos\": [{\"content\": \"student_score.id='小明'\", \"score\": \"0.83\"}], \"schema_infos\": [{\"table_id\": \"student_score\", \"table_desc\": \"学生分数表\", \"columns\": [{\"col_name\": \"id\", \"col_caption\": \"学生id\", \"col_desc\": \"例值为:1,2,3\", \"col_type\": \"string\"}, {\"col_name\": \"name\", \"col_caption\": \"学生姓名\", \"col_desc\": \"例值为:张三,李四,小明\", \"col_type\": \"string\"}, {\"col_name\": \"score\", \"col_caption\": \"学生分数\", \"col_desc\": \"例值为:98,100,66\", \"col_type\": \"string\"}]}]}";
    messageParams.add(new AttachmentRoleMessageParam(JsonUtils.parse(meta)));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.NL2SQL)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    CodeGenerationResult result = generation.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, nl2sqlExpectTextBody, rsp);
  }

  @Test
  public void testCustomSampleWithCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694702346.730724);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "以下是生成Python函数的代码：\\n\\n```python\\ndef file_size(path):\\n    total_size = 0\\n    for root, dirs, files in os.walk(path):\\n        for file in files:\\n            full_path = os.path.join(root, file)\\n            total_size += os.path.getsize(full_path)\\n    return total_size\\n```\\n\\n函数名为`file_size`，输入参数是给定路径`path`。函数通过递归遍历给定路径下的所有文件，使用`os.walk`函数遍历根目录及其子目录下的文件，计算每个文件的大小并累加到总大小上。最后，返回总大小作为函数的返回值。");
    choice.addProperty("frame_id", 25);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 46);
    usage.addProperty("output_tokens", 198);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("bf321b27-a3ff-9674-a70e-be5f40a435e4")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CUSTOM)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
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
    RecordedRequest request = server.takeRequest();
    semaphore.acquire();
    checkResult(results.get(0), request, customExpectTextBody, rsp);
  }

  @Test
  public void testCustomSampleWithStream()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694702346.730724);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "以下是生成Python函数的代码：\\n\\n```python\\ndef file_size(path):\\n    total_size = 0\\n    for root, dirs, files in os.walk(path):\\n        for file in files:\\n            full_path = os.path.join(root, file)\\n            total_size += os.path.getsize(full_path)\\n    return total_size\\n```\\n\\n函数名为`file_size`，输入参数是给定路径`path`。函数通过递归遍历给定路径下的所有文件，使用`os.walk`函数遍历根目录及其子目录下的文件，计算每个文件的大小并累加到总大小上。最后，返回总大小作为函数的返回值。");
    choice.addProperty("frame_id", 25);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 46);
    usage.addProperty("output_tokens", 198);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("bf321b27-a3ff-9674-a70e-be5f40a435e4")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CUSTOM)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    CodeGeneration generation = new CodeGeneration();
    Flowable<CodeGenerationResult> flowable = generation.streamCall(param);
    CodeGenerationResult result = flowable.blockingSingle();
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, customExpectTextBody, rsp);
  }

  @Test
  public void testCustomSampleWithStreamCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    JsonObject output = new JsonObject();
    JsonArray choices = new JsonArray();
    JsonObject choice = new JsonObject();
    choice.addProperty("finish_reason", "stop");
    choice.addProperty("frame_timestamp", 1694702346.730724);
    choice.addProperty("index", 0);
    choice.addProperty(
        "content",
        "以下是生成Python函数的代码：\\n\\n```python\\ndef file_size(path):\\n    total_size = 0\\n    for root, dirs, files in os.walk(path):\\n        for file in files:\\n            full_path = os.path.join(root, file)\\n            total_size += os.path.getsize(full_path)\\n    return total_size\\n```\\n\\n函数名为`file_size`，输入参数是给定路径`path`。函数通过递归遍历给定路径下的所有文件，使用`os.walk`函数遍历根目录及其子目录下的文件，计算每个文件的大小并累加到总大小上。最后，返回总大小作为函数的返回值。");
    choice.addProperty("frame_id", 25);
    choices.add(choice);
    output.add("choices", choices);
    JsonObject usage = new JsonObject();
    usage.addProperty("input_tokens", 46);
    usage.addProperty("output_tokens", 198);
    TestResponse rsp =
        TestResponse.builder()
            .requestId("bf321b27-a3ff-9674-a70e-be5f40a435e4")
            .output(output)
            .usage(usage)
            .build();
    server.enqueue(
        new MockResponse()
            .setBody("data: " + JsonUtils.toJson(rsp) + "\n\n")
            .setHeader("content-type", MEDIA_TYPE_EVENT_STREAM));
    int port = server.getPort();
    List<MessageParamBase> messageParams = new ArrayList<>();
    messageParams.add(new UserRoleMessageParam("根据下面的功能描述生成一个python函数。代码的功能是计算给定路径下所有文件的总大小。"));
    CodeGenerationParam param =
        CodeGenerationParam.builder()
            .model(MODEL)
            .scene(CodeGeneration.Scenes.CUSTOM)
            .message(messageParams)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
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
    RecordedRequest request = server.takeRequest();
    semaphore.acquire();
    checkResult(results.get(0), request, customExpectTextBody, rsp);
  }
}
