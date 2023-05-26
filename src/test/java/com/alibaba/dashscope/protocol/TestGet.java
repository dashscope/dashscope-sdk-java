package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.Protocol;
import com.alibaba.dashscope.mock.MockServer;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.google.common.collect.Maps;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
public class TestGet {

  private final MockServer mockServer = new MockServer();

  private String oldBaseHttpApiUrl = null;

  private String oldBaseWebsocketApiUrl = null;

  private static final String testGetUrl = "tasks";

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

  @Test
  public void testGet() {
    String url = Constants.baseHttpApiUrl + "/" + testGetUrl;
    Map<String, String> headers = Maps.newHashMap();
    headers.put("Authorization", "testKey");
    Response response =
        ServiceFacility.streamingNone(
            Protocol.HTTP, url, headers, Request.builder().build(), HttpMethod.GET, 30);
    log.info(JsonUtils.toJson(response));
  }
}
