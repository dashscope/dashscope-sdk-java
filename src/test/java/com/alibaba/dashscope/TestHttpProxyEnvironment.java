// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.Constants;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Disabled
@Execution(ExecutionMode.SAME_THREAD)
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestHttpProxyEnvironment {
  private MockWebServer mockServer;

  @BeforeEach
  public void before() throws IOException {
    System.out.println("Starting server!");
    this.mockServer = new MockWebServer();
    this.mockServer.start(38888);
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s/api/v1/", mockServer.getPort());
    Constants.apiKey = "1234";
  }

  @AfterEach
  public void after() throws IOException {
    this.mockServer.close();
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_PROXY_HOST", value = "localhost")
  @SetEnvironmentVariable(key = "DASHSCOPE_PROXY_PORT", value = "9887")
  public void testSetProxyWithEnv() throws NoApiKeyException, IOException {
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("timeout/connection"));
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>();
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();
    MockResponse mockResponse = new MockResponse().setBody("{\"model\": \"m\"}");
    mockServer.enqueue(mockResponse);
    Exception exception =
        assertThrows(
            ApiException.class,
            () -> {
              api.call(param, serviceOption);
            });
    System.out.println(exception.getMessage());
    assertTrue(exception.getMessage().contains("network error"));
    mockServer.close();
  }
}
