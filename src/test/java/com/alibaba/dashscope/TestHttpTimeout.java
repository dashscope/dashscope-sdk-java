package com.alibaba.dashscope;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.Constants;
import java.io.IOException;
import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestHttpTimeout {
  private MockWebServer mockServer;

  public TestHttpTimeout() {}

  @Before
  public void before() throws IOException {
    System.out.println("Starting server!");
    this.mockServer = new MockWebServer();
    this.mockServer.start();
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s/api/v1/", mockServer.getPort());
    Constants.apiKey = "1234";
  }

  @After
  public void after() throws IOException {
    this.mockServer.close();
  }

  @Test
  public void testConnectionTimeout() throws ApiException, NoApiKeyException {
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("timeout/connection"));
    long timeoutSeconds = 10;
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(
                Duration.ofSeconds(timeoutSeconds)) // set connection timeout, default 120s
            .readTimeout(Duration.ofSeconds(20)) // set read timeout, default 300s
            .writeTimeout(Duration.ofSeconds(20)) // set read timeout, default 60s
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();
    long delayTime = 11;
    MockResponse mockResponse = new MockResponse().setHeadersDelay(delayTime, TimeUnit.SECONDS);
    this.mockServer.enqueue(mockResponse);
    long start = System.currentTimeMillis();
    Exception exception =
        assertThrows(
            ApiException.class,
            () -> {
              api.call(param, serviceOption);
            });
    long end = System.currentTimeMillis();
    System.out.println(exception.getMessage());
    assertTrue(exception.getMessage().contains("unknown_error"));
    assertTrue(end - start > timeoutSeconds * 1000);
  }

  @Test
  public void testReadTimeout() throws ApiException, NoApiKeyException {
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("timeout/connection"));
    long timeoutSeconds = 10;
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(20)) // set connection timeout, default 120s
            .readTimeout(Duration.ofSeconds(timeoutSeconds)) // set read timeout, default 300s
            .writeTimeout(Duration.ofSeconds(20)) // set read timeout, default 60s
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();
    MockResponse mockResponse = new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE);
    mockServer.enqueue(mockResponse);
    long start = System.currentTimeMillis();
    Exception exception =
        assertThrows(
            ApiException.class,
            () -> {
              api.call(param, serviceOption);
            });
    long end = System.currentTimeMillis();
    System.out.println(exception.getMessage());
    assertTrue(exception.getMessage().contains("unknown_error"));
    assertTrue(end - start > timeoutSeconds * 1000);
  }

  @Test
  public void testWriteTimeout() throws ApiException, NoApiKeyException {
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath(String.format("timeout/connection"));
    long timeoutMillisSeconds = 1;
    // write timeout, time out of client send data, if the data is too many to send in
    // write timeout, such as 1G send in 1 second, will write timeout
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(20)) // set connection timeout, default 120s
            .readTimeout(Duration.ofSeconds(20)) // set read timeout, default 300s
            .writeTimeout(Duration.ofMillis(timeoutMillisSeconds))
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);

    int leftLimit = 97; // letter 'a'
    int rightLimit = 122; // letter 'z'
    int targetStringLength = 100000000;
    Random random = new Random();

    String generatedString =
        random
            .ints(leftLimit, rightLimit + 1)
            .limit(targetStringLength)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();

    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name(generatedString).description("desc").build();
    MockResponse mockResponse = new MockResponse().setBody("{\"model\": \"m\"}");
    mockServer.enqueue(mockResponse);
    long start = System.currentTimeMillis();
    Exception exception =
        assertThrows(
            ApiException.class,
            () -> {
              api.call(param, serviceOption);
              okhttp3.mockwebserver.RecordedRequest request = mockServer.takeRequest();
              System.out.println(request.getBody());
            });
    long end = System.currentTimeMillis();
    assertTrue(exception.getMessage().contains("timeout"));
    assertTrue(end - start > timeoutMillisSeconds);
  }
}
