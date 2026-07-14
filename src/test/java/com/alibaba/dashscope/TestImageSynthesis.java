package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import java.io.IOException;
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
public class TestImageSynthesis {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;
  private String expectRequestBody =
      "{\"model\":\"wanx2.1-imageedit\",\"input\":{\"prompt\":\"雄鹰自由自在的在蓝天白云下飞翔\",\"function\":\"description_edit_with_mask\",\"base_image_url\":\"https://www.xxx.cn/b.png\",\"mask_image_url\":\"https://www.xxx.cn/a.png\"},\"parameters\":{\"size\":\"1024*1024\",\"n\":4}}";

  @BeforeEach
  public void before() {
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  @Test
  public void testImageSynthesisNormal()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"39\",\"output\":{\"task_id\":\"e4\",\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://1\"},{\"url\":\"https://2\"},{\"url\":\"https://\"},{\"url\":\"https://4\"}],\"task_metrics\":{\"TOTAL\":4,\"SUCCEEDED\":4,\"FAILED\":0}},\"usage\":{\"image_count\":4},\"headers\":{\"content-length\":\"365\",\"content-type\":\"application/json; charset=utf-8\"},\"status_code\":200,\"code\":\"\",\"message\":\"\"}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_2_1_IMAGEEDIT)
            .n(4)
            .function("description_edit_with_mask")
            .maskImageUrl("https://www.xxx.cn/a.png")
            .baseImageUrl("https://www.xxx.cn/b.png")
            .size("1024*1024")
            .prompt("雄鹰自由自在的在蓝天白云下飞翔")
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    ImageSynthesisResult result = is.asyncCall(param);
    String resultJson = JsonUtils.toJson(result);
    System.out.println(resultJson);
    assertEquals(resultJson, responseBody);
    RecordedRequest request = server.takeRequest();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/aigc/image2image/image-synthesis");
    String requestBody = request.getBody().readUtf8();
    assertEquals(expectRequestBody, requestBody);
  }

  @Test
  public void testImageSynthesisUsageMore()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"39\",\"output\":{\"task_id\":\"e4\",\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://1\"},{\"url\":\"https://2\"},{\"url\":\"https://\"},{\"url\":\"https://4\"}],\"task_metrics\":{\"TOTAL\":4,\"SUCCEEDED\":4,\"FAILED\":0}},\"usage\":{\"image_count\":4,\"size\":\"1024*1024\"}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_2_1_IMAGEEDIT)
            .n(4)
            .function("description_edit_with_mask")
            .maskImageUrl("https://www.xxx.cn/a.png")
            .baseImageUrl("https://www.xxx.cn/b.png")
            .size("1024*1024")
            .prompt("雄鹰自由自在的在蓝天白云下飞翔")
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    ImageSynthesisResult result = is.asyncCall(param);
    String resultJson = JsonUtils.toJson(result);
    System.out.println(resultJson); // usage has more field no error
    RecordedRequest request = server.takeRequest();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/aigc/image2image/image-synthesis");
    String requestBody = request.getBody().readUtf8();
    assertEquals(expectRequestBody, requestBody);
  }

  @Test
  public void testImageSynthesisWithRefStrengthAndRefMode()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"40\",\"output\":{\"task_id\":\"e5\",\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://ref1\"}],\"task_metrics\":{\"TOTAL\":1,\"SUCCEEDED\":1,\"FAILED\":0}},\"usage\":{\"image_count\":1}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_2_1_IMAGEEDIT)
            .n(1)
            .prompt("参考图像生成")
            .refStrength(0.8f)
            .refMode("repaint")
            .images(java.util.Arrays.asList("https://example.com/ref.png"))
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    ImageSynthesisResult result = is.asyncCall(param);
    RecordedRequest request = server.takeRequest();
    String requestBody = request.getBody().readUtf8();
    log.info("Request body with ref params: {}", requestBody);
    // 验证 ref_strength 和 ref_mode 参数被正确序列化
    assertEquals(true, requestBody.contains("\"ref_strength\":0.8"));
    assertEquals(true, requestBody.contains("\"ref_mode\":\"repaint\""));
  }

  @Test
  public void testImageSynthesisWithMaskColor()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"41\",\"output\":{\"task_id\":\"e6\",\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://mask1\"}],\"task_metrics\":{\"TOTAL\":1,\"SUCCEEDED\":1,\"FAILED\":0}},\"usage\":{\"image_count\":1}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_2_1_IMAGEEDIT)
            .n(1)
            .prompt("局部重绘")
            .function("description_edit_with_mask")
            .baseImageUrl("https://www.xxx.cn/base.png")
            .maskImageUrl("https://www.xxx.cn/mask.png")
            .maskColor("#FFFFFF")
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    ImageSynthesisResult result = is.asyncCall(param);
    RecordedRequest request = server.takeRequest();
    String requestBody = request.getBody().readUtf8();
    log.info("Request body with mask_color: {}", requestBody);
    // 验证 mask_color 参数被正确序列化
    assertEquals(true, requestBody.contains("\"mask_color\":\"#FFFFFF\""));
  }

  @Test
  public void testImageSynthesisWithPromptExtendAndWatermark()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"42\",\"output\":{\"task_id\":\"e7\",\"task_status\":\"SUCCEEDED\",\"results\":[{\"url\":\"https://extend1\"}],\"task_metrics\":{\"TOTAL\":1,\"SUCCEEDED\":1,\"FAILED\":0}},\"usage\":{\"image_count\":1}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    ImageSynthesis is = new ImageSynthesis();
    ImageSynthesisParam param =
        ImageSynthesisParam.builder()
            .model(ImageSynthesis.Models.WANX_2_1_IMAGEEDIT)
            .n(1)
            .prompt("简单提示词")
            .promptExtend(true)
            .watermark(false)
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    ImageSynthesisResult result = is.asyncCall(param);
    RecordedRequest request = server.takeRequest();
    String requestBody = request.getBody().readUtf8();
    log.info("Request body with prompt_extend and watermark: {}", requestBody);
    // 验证 prompt_extend 和 watermark 参数被正确序列化
    assertEquals(true, requestBody.contains("\"prompt_extend\":true"));
    assertEquals(true, requestBody.contains("\"watermark\":false"));
  }
}
