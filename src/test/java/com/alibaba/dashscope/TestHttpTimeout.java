package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.api.GeneralApi;
import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.protocol.GeneralServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.utils.Constants;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class TestHttpTimeout {
  private MockWebServer mockServer;
  private String originalBaseHttpApiUrl;

  @BeforeEach
  public void before() throws IOException {
    this.mockServer = new MockWebServer();
    this.mockServer.start();
    originalBaseHttpApiUrl = Constants.baseHttpApiUrl;
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s/api/v1/", mockServer.getPort());
    Constants.apiKey = "1234";
  }

  @AfterEach
  public void after() throws IOException {
    Constants.baseHttpApiUrl = originalBaseHttpApiUrl;
    this.mockServer.close();
  }

  private static GeneralServiceOption postOption() {
    GeneralServiceOption serviceOption = GeneralServiceOption.builder().build();
    serviceOption.setHttpMethod(HttpMethod.POST);
    serviceOption.setPath("timeout/connection");
    return serviceOption;
  }

  private static void assertServiceUnavailable(ApiException exception) {
    assertEquals(
        PublicErrorCode.SERVICE_UNAVAILABLE.getStatusCode(), exception.getStatus().getStatusCode());
    assertEquals(
        PublicErrorCode.SERVICE_UNAVAILABLE.getErrorCode(), exception.getStatus().getCode());
  }

  @Test
  public void testConnectionTimeout() throws ApiException, NoApiKeyException {
    // RFC 5737 TEST-NET-1, guaranteed unroutable: the connect attempt hangs until the
    // connect timeout fires.
    Constants.baseHttpApiUrl = "http://192.0.2.1:81/api/v1/";
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(2))
            .readTimeout(Duration.ofSeconds(20))
            .writeTimeout(Duration.ofSeconds(20))
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();

    ApiException exception = assertThrows(ApiException.class, () -> api.call(param, postOption()));

    System.out.println(exception.getMessage());
    assertServiceUnavailable(exception);
  }

  @Test
  public void testReadTimeout() throws ApiException, NoApiKeyException {
    long timeoutSeconds = 3;
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(timeoutSeconds))
            .writeTimeout(Duration.ofSeconds(20))
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    TimeoutTestParam param =
        TimeoutTestParam.builder().model("model").name("test").description("desc").build();
    mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    long start = System.currentTimeMillis();
    ApiException exception = assertThrows(ApiException.class, () -> api.call(param, postOption()));
    long elapsed = System.currentTimeMillis() - start;

    System.out.println(exception.getMessage());
    assertServiceUnavailable(exception);
    assertTrue(elapsed >= timeoutSeconds * 1000);
  }

  @Test
  public void testWriteTimeout() throws ApiException, NoApiKeyException {
    ConnectionOptions connectionOptions =
        ConnectionOptions.builder()
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .writeTimeout(Duration.ofMillis(1))
            .build();
    GeneralApi<HalfDuplexParamBase> api = new GeneralApi<>(connectionOptions);
    // The server never reads the request body; an 8MB payload cannot fit the socket buffers,
    // so the 1ms write timeout must fire.
    char[] chars = new char[8 * 1024 * 1024];
    Arrays.fill(chars, 'a');
    TimeoutTestParam param =
        TimeoutTestParam.builder()
            .model("model")
            .name(new String(chars))
            .description("desc")
            .build();
    mockServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    ApiException exception = assertThrows(ApiException.class, () -> api.call(param, postOption()));

    System.out.println(exception.getMessage());
    assertServiceUnavailable(exception);
  }
}
