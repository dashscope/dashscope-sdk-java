// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.Constants;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@Slf4j
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestHttpProxySettingWithCode {
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
  public void testSetProxyNoServer() throws NoApiKeyException {
    // assertEquals(addr.getHostName(), "https://host.com");
    // assertEquals(addr.getPort(), 9887);
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("timeout/connection"));
    long timeoutSeconds = 10;
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds)) // set
            // connection
            // timeout,
            // default
            // 120s
            .readTimeout(Duration.ofSeconds(20)) // set read timeout, default 300s
            .writeTimeout(Duration.ofSeconds(20)) // set read timeout, default 60s
            .proxyHost("127.0.0.1")
            .proxyPort(37777)
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();
    MockResponse mockResponse = new MockResponse().setBody("{\"model\": \"m\"}");
    this.mockServer.enqueue(mockResponse);
    Exception exception =
        assertThrows(
            ApiException.class,
            () -> {
              api.call(param, serviceOption);
            });
    System.out.println(exception.getMessage());
    assertTrue(exception.getMessage().contains("network error"));
  }

  @Test
  public void testSetProxy() throws NoApiKeyException {
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("timeout/connection"));
    long timeoutSeconds = 10;
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(timeoutSeconds)) // set
            // connection
            // timeout,
            // default
            // 120s
            .readTimeout(Duration.ofSeconds(20)) // set read timeout, default 300s
            .writeTimeout(Duration.ofSeconds(20)) // set read timeout, default 60s
            .proxyHost("127.0.0.1")
            .proxyPort(38888)
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();
    MockResponse mockResponse = new MockResponse().setBody("{\"model\": \"m\"}");
    this.mockServer.enqueue(mockResponse);

    DashScopeResult result = api.call(param, serviceOption);
    System.out.println(result);
  }
}
