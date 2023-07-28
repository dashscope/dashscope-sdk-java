package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingOutput;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.embeddings.TextEmbeddingUsage;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
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
public class TestTextEmbedding {
  private static final MediaType MEDIA_TYPE_APPLICATION_JSON =
      MediaType.parse("application/json; charset=utf-8");
  MockWebServer server;
  private TestResponse rsp;
  private TextEmbeddingOutput output;
  private TextEmbeddingUsage usage;
  private String requestId;
  private String expectBody =
      "{\"model\":\"text-embedding-v1\",\"input\":{\"texts\":[\"风急天高猿啸哀\",\"渚清沙白鸟飞回\",\"无边落木萧萧下\",\"不尽长江滚滚来\"]}}";

  @BeforeEach
  public void before() {
    // {"output":{"embeddings":[{"embedding":[1.5536729097366333],"text_index":0},{"embedding":[1.1659477949142456,-0.7178032994270325],"text_index":1},{"embedding":[2.6992974281311035,2.5143065452575684,0.20655924081802368],"text_index":2},{"embedding":[-1.2515695095062256,2.7955071926116943,1.8544834852218628,-2.3869540691375732,1.76776123046875],"text_index":3}]},
    // "usage":{"total_tokens":26},
    // "request_id":"83bf1c9e-d50d-9abd-a4bc-bed2c7749d59"}
    output = new TextEmbeddingOutput();
    String embeddings =
        "[{\"embedding\":[1.5536729097366333],\"text_index\":0},{\"embedding\":[1.1659477949142456,-0.7178032994270325],\"text_index\":1},{\"embedding\":[2.6992974281311035,2.5143065452575684,0.20655924081802368],\"text_index\":2},{\"embedding\":[-1.2515695095062256,2.7955071926116943,1.8544834852218628,-2.3869540691375732,1.76776123046875],\"text_index\":3}]";
    Type tp = new TypeToken<ArrayList<TextEmbeddingResultItem>>() {}.getType();
    output.setEmbeddings(JsonUtils.fromJson(embeddings, tp));
    usage = new TextEmbeddingUsage();
    usage.setTotalTokens(26);
    requestId = "682d7353-5100-9054-b44b-76f0cb045b37";
    rsp =
        TestResponse.builder()
            .output(JsonUtils.toJsonObject(output))
            .usage(JsonUtils.toJsonObject(usage))
            .requestId(requestId)
            .build();
    this.server = new MockWebServer();
  }

  @AfterEach
  public void after() throws IOException {
    server.close();
  }

  private void checkResult(TextEmbeddingResult result, RecordedRequest request) {
    assertEquals(result.getRequestId(), requestId);
    assertEquals(result.getOutput(), output);
    assertEquals(result.getUsage(), usage);
    String body = request.getBody().readUtf8();
    assertEquals(request.getMethod(), "POST");
    assertEquals(request.getPath(), "/services/embeddings/text-embedding/text-embedding");
    assertEquals(expectBody, body);
  }

  @Test
  public void testEmbeddings()
      throws ApiException, NoApiKeyException, IOException, InterruptedException,
          InputRequiredException {
    server.enqueue(
        new MockResponse()
            .setBody(JsonUtils.toJson(rsp))
            .setHeader("content-type", MEDIA_TYPE_APPLICATION_JSON));
    int port = server.getPort();
    TextEmbeddingParam param =
        TextEmbeddingParam.builder()
            .model(TextEmbedding.Models.TEXT_EMBEDDING_V1)
            .texts(Arrays.asList("风急天高猿啸哀", "渚清沙白鸟飞回", "无边落木萧萧下", "不尽长江滚滚来"))
            .build();
    TextEmbedding textEmbedding = new TextEmbedding();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", port);
    TextEmbeddingResult result = textEmbedding.call(param);
    RecordedRequest request = server.takeRequest();
    checkResult(result, request);
  }
}
