// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol;

import static com.alibaba.dashscope.utils.ApiKeywords.OUTPUT;

import com.alibaba.dashscope.common.Param;
import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.common.Result;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.mock.MockServer;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.gson.JsonObject;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
public class TestAsyncTask {

  private final MockServer mockServer = new MockServer();

  private String oldBaseHttpApiUrl = null;

  private String oldBaseWebsocketApiUrl = null;

  @BeforeEach
  public void setUp() {
    oldBaseHttpApiUrl = Constants.baseHttpApiUrl;
    oldBaseWebsocketApiUrl = Constants.baseWebsocketApiUrl;
    Constants.baseHttpApiUrl = "http://localhost:8080/api/v1";
    Constants.baseWebsocketApiUrl = "ws://localhost:8080/api-ws/v1/inference";
    mockServer.start(0, "testKey");
  }

  @AfterEach
  public void tearDown() {
    Constants.baseHttpApiUrl = oldBaseHttpApiUrl;
    Constants.baseWebsocketApiUrl = oldBaseWebsocketApiUrl;
    mockServer.tearDown();
  }

  @SuperBuilder
  static class TestParam extends Param {

    @Builder.Default private String testParam = "test";

    @Override
    public String url() {
      return "/services/audio3/asr/transcribe";
    }

    @Override
    public Request toRequest(Protocol protocol) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("param1", testParam);
      return Request.builder().message(JsonUtils.toJson(jsonObject)).build();
    }

    @Override
    public Class<? extends Result> resultType() {
      return TestResult.class;
    }
  }

  static class TestResult extends Result {

    String message;

    @Override
    public void fromResponse(Protocol protocol, Response response) throws ApiException {
      JsonObject jsonObject = JsonUtils.parse(response.getMessage());
      message = jsonObject.getAsJsonObject(OUTPUT).get("param1").getAsString();
    }
  }

  @Test
  public void testAsyncTask() {
    var param = TestParam.builder().apiKey("testKey").build();
    TestResult result = (TestResult) ServiceFacility.callAndWait(null, param, HttpMethod.POST, 30);
    assert (result.message.equals(param.testParam));
  }
}
