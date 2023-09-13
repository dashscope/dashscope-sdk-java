package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;

import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.nlp.understanding.Understanding;
import com.alibaba.dashscope.nlp.understanding.UnderstandingParam;
import com.alibaba.dashscope.nlp.understanding.UnderstandingResult;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
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
public class TestUnderstanding {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  private static final MediaType MEDIA_TYPE_EVENT_STREAM = MediaType.parse("text/event-stream");

  MockWebServer server;
  private TestResponse rsp;
  private JsonObject output;
  private JsonObject usage;
  private String requestId;
  private String expectTextBody =
      "{\"model\":\"opennlu-v1\",\"input\":{\"sentence\":\"老师今天表扬我了\",\"labels\":\"积极，消极\",\"task\":\"classification\"},\"parameters\":{}}";

  @BeforeEach
  public void before() {
    output = new JsonObject();
    output.addProperty("text", "积极;");
    output.addProperty("rt", 0.0793976578861475D);
    usage = new JsonObject();
    usage.addProperty("input_tokens", 20);
    usage.addProperty("output_tokens", 2);
    usage.addProperty("total_tokens", 22);
    requestId = "2fa27453-f840-9815-85a9-72cb1e0b1fdf";
    rsp = TestResponse.builder().output(output).usage(usage).requestId(requestId).build();
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  private void checkResult(UnderstandingResult result, RecordedRequest request, String expectBody) {
    //        assertEquals(result.getRequestId(), requestId);
    assertEquals(result.getOutput().getText(), output.get("text").getAsString());
    assertEquals(
        result.getUsage().getInputTokens(), new Integer(usage.get("input_tokens").getAsInt()));
    assertEquals(
        result.getUsage().getOutputTokens(), new Integer(usage.get("output_tokens").getAsInt()));
    String body = request.getBody().readUtf8();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/nlp/nlu/understanding");
    assertEquals(expectBody, body);
  }

  @Test
  public void testHttpTextCall()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    UnderstandingParam param =
        UnderstandingParam.builder()
            .model(Understanding.Models.OPENNLU_V1)
            .sentence("老师今天表扬我了")
            .labels("积极，消极")
            .task("classification")
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Understanding understanding = new Understanding();
    UnderstandingResult result = understanding.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request, expectTextBody);
  }

  @Test
  public void testHttpCallWithCallBack()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    UnderstandingParam param =
        UnderstandingParam.builder()
            .model(Understanding.Models.OPENNLU_V1)
            .sentence("老师今天表扬我了")
            .labels("积极，消极")
            .task("classification")
            .build();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    Understanding understanding = new Understanding();
    Semaphore semaphore = new Semaphore(0);
    List<UnderstandingResult> results = new ArrayList<>();
    understanding.call(
        param,
        new ResultCallback<UnderstandingResult>() {
          @Override
          public void onEvent(UnderstandingResult msg) {
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
    checkResult(results.get(0), request, expectTextBody);
  }
}
