package com.alibaba.dashscope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.embeddings.BatchTextEmbedding;
import com.alibaba.dashscope.embeddings.BatchTextEmbeddingParam;
import com.alibaba.dashscope.embeddings.BatchTextEmbeddingResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskListParam;
import com.alibaba.dashscope.task.AsyncTaskListResult;
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
public class TestBatchTextEmbedding {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;
  private String expectRequestBody =
      "{\"model\":\"pre-offline-file-embedding\",\"input\":{\"url\":\"https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt\"},\"parameters\":{\"text_type\":\"document\"}}";

  @BeforeEach
  public void before() {
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testCreateAsyncTask()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"78a74ba9-b8eb-9ca5-ab34-5a56f453cf03\",\"output\":{\"task_id\":\"2a1d8589-7148-422a-b9e7-f41682f07160\",\"task_status\":\"PENDING\"}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    BatchTextEmbeddingParam param =
        BatchTextEmbeddingParam.builder()
            .model("pre-offline-file-embedding")
            .url("https://modelscope.oss-cn-beijing.aliyuncs.com/resource/text_embedding_file.txt")
            .build();
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    BatchTextEmbeddingResult result = textEmbedding.asyncCall(param);
    System.out.println(JsonUtils.toJson(result));
    assertEquals(responseBody, JsonUtils.toJson(result));
    RecordedRequest request = server.takeRequest();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/embeddings/text-embedding/text-embedding");
    String requestBody = request.getBody().readUtf8();
    assertEquals(expectRequestBody, requestBody);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testFetchTask()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"78a74ba9-b8eb-9ca5-ab34-5a56f453cf03\",\"output\":{\"task_id\":\"2a1d8589-7148-422a-b9e7-f41682f07160\",\"task_status\":\"PENDING\"}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    BatchTextEmbeddingResult result =
        textEmbedding.fetch("2a1d8589-7148-422a-b9e7-f41682f07160", null);
    System.out.println(JsonUtils.toJson(result));
    assertEquals(responseBody, JsonUtils.toJson(result));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void testFetchTaskWithDiffUrl()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    String responseBody =
        "{\"request_id\":\"78a74ba9-b8eb-9ca5-ab34-5a56f453cf03\",\"output\":{\"task_id\":\"2a1d8589-7148-422a-b9e7-f41682f07160\",\"task_status\":\"PENDING\"}}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    String url = String.format("http://127.0.0.1:%s/customurl", port);
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding(url);
    BatchTextEmbeddingResult result =
        textEmbedding.fetch("2a1d8589-7148-422a-b9e7-f41682f07160", null);
    System.out.println(JsonUtils.toJson(result));
    assertEquals(responseBody, JsonUtils.toJson(result));
    RecordedRequest req = server.takeRequest();
    // verify call customurl
    assertEquals(req.getPath(), "/customurl/tasks/2a1d8589-7148-422a-b9e7-f41682f07160");
  }

  @Test
  public void testListParameters() throws ApiException, NoApiKeyException, InterruptedException {
    AsyncTaskListParam param =
        AsyncTaskListParam.builder()
            .pageNo(100)
            .pageSize(1000)
            .startTime("1691561390000")
            .endTime("1691561396394")
            .modelName("modelName")
            .apiKeyId("1")
            .region("beijing")
            .status("SUCCEEDED")
            .build();
    String responseBody =
        "{\"request_id\":\"31a80745-990d-958b-ad1c-fd51f17a6996\",\"data\":[{\"api_key_id\":\"1\",\"caller_parent_id\":\"2\",\"caller_uid\":\"3\",\"end_time\":1691561396394,\"gmt_create\":1691561394828,\"model_name\":\"pre-offline-file-embedding\",\"region\":\"cn-beijing\",\"request_id\":\"5ddcdba0-9b22-93c1-946e-1eb152b77efa\",\"start_time\":1691561395295,\"status\":\"SUCCEEDED\",\"task_id\":\"bb7c1bdb-d8de-4619-83b8-9ad3c3313def\",\"user_api_unique_key\":\"apikey:v1:embeddings:text-embedding:text-embedding:pre-offline-file-embedding\"}],\"total\":1,\"total_page\":1,\"page_no\":1,\"page_size\":10}";
    server.enqueue(
        new MockResponse()
            .setBody(responseBody)
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    BatchTextEmbedding textEmbedding = new BatchTextEmbedding();
    AsyncTaskListResult result = textEmbedding.list(param);
    assertEquals(responseBody, JsonUtils.toJson(result));
    RecordedRequest request = server.takeRequest();
    assertEquals(request.getMethod(), "GET");
    String path = request.getPath();
    assertTrue(path.startsWith("/tasks"));
    assertTrue(path.indexOf("start_time=1691561390000") > 0);
    assertTrue(path.indexOf("model_name=modelName") > 0);
    assertTrue(path.indexOf("end_time=1691561396394") > 0);
    assertTrue(path.indexOf("page_no=100") > 0);
    assertTrue(path.indexOf("region=beijing") > 0);
    assertTrue(path.indexOf("api_key_id=1") > 0);
    assertTrue(path.indexOf("status=SUCCEEDED") > 0);
    assertTrue(path.indexOf("page_size=1000") > 0);
  }
}
