package com.alibaba.dashscope;

import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesis;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisParam;
import com.alibaba.dashscope.aigc.videosynthesis.VideoSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

import java.io.IOException;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestVideoSynthesis {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
          MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;

  @BeforeEach
  public void before() {
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  @Test
  public void testVideoSynthesisNormal()
          throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
            "{\"request_id\":\"39\",\"output\":{\"task_id\":\"e4\",\"task_status\":\"SUCCEEDED\",\"video_url\":\"https://1\"},\"usage\":{\"video_count\":1}}";
    assert MEDIA_TYPE_APPLICATION_JSON != null;
    server.enqueue(
            new MockResponse()
                    .setBody(responseBody)
                    .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    VideoSynthesis is = new VideoSynthesis();
    VideoSynthesisParam param =
            VideoSynthesisParam.builder()
                    .model(VideoSynthesis.Models.WANX_KF2V)
                    .firstFrameUrl("https://www.xxx.cn/a.png")
                    .lastFrameUrl("https://www.xxx.cn/b.png")
                    .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    VideoSynthesisResult result = is.asyncCall(param);
    String resultJson = JsonUtils.toJson(result);
    System.out.println(resultJson);
    Assertions.assertEquals(responseBody, resultJson);
    RecordedRequest request = server.takeRequest();
    Assertions.assertEquals("POST", request.getMethod());
    Assertions.assertEquals("/services/aigc/image2video/video-synthesis", request.getPath());
    String requestBody = request.getBody().readUtf8();
    System.out.println(requestBody);
    String expectRequestBody =
            "{\"model\":\"wanx-kf2v\",\"input\":{\"extend_prompt\":true,\"first_frame_url\":\"https://www.xxx.cn/a.png\",\"last_frame_url\":\"https://www.xxx.cn/b.png\"},\"parameters\":{\"duration\":5,\"with_audio\":false,\"size\":\"1280*720\",\"resolution\":\"720P\"}}";
    Assertions.assertEquals(expectRequestBody, requestBody);
  }

  @Test
  public void testVideoSynthesisUsageMore()
          throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
            "{\"request_id\":\"39\",\"output\":{\"task_id\":\"e4\",\"task_status\":\"SUCCEEDED\",\"video_url\":\"https://1\"},\"usage\":{\"video_count\":1}}";
    assert MEDIA_TYPE_APPLICATION_JSON != null;
    server.enqueue(
            new MockResponse()
                    .setBody(responseBody)
                    .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    VideoSynthesis is = new VideoSynthesis();
    VideoSynthesisParam param =
            VideoSynthesisParam.builder()
                    .model(VideoSynthesis.Models.WANX_KF2V)
                    .firstFrameUrl("https://www.xxx.cn/a.png")
                    .lastFrameUrl("https://www.xxx.cn/b.png")
                    .duration(4)
                    .seed(1234)
                    .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    VideoSynthesisResult result = is.asyncCall(param);
    String resultJson = JsonUtils.toJson(result);
    System.out.println(resultJson); // usage has more field no error
    RecordedRequest request = server.takeRequest();
    Assertions.assertEquals("POST", request.getMethod());
    Assertions.assertEquals("/services/aigc/image2video/video-synthesis", request.getPath());
    String requestBody = request.getBody().readUtf8();
    System.out.println(requestBody);
    String expectRequestBody =
            "{\"model\":\"wanx-kf2v\",\"input\":{\"extend_prompt\":true,\"first_frame_url\":\"https://www.xxx.cn/a.png\",\"last_frame_url\":\"https://www.xxx.cn/b.png\"},\"parameters\":{\"duration\":4,\"with_audio\":false,\"size\":\"1280*720\",\"seed\":1234,\"resolution\":\"720P\"}}";
    Assertions.assertEquals(expectRequestBody, requestBody);
  }
}
