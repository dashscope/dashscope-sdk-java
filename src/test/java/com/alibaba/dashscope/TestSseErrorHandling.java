// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.DashScopeResult;
import com.alibaba.dashscope.common.OutputMode;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ApiServiceOption;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.Constants;
import io.reactivex.Flowable;
import io.reactivex.plugins.RxJavaPlugins;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Execution(ExecutionMode.SAME_THREAD)
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "1234")
public class TestSseErrorHandling {
  private static final String ERROR_JSON =
      "{\"code\":\"InvalidParameter\",\"message\":\"url error, please check url"
          + "\uFF01\",\"request_id\":\"ffbb90bf-1b08-9bfc-9cb3-94eeee5a9258\"}";

  private SynchronizeHalfDuplexApi<HalfDuplexTestParam> syncApi;
  private final List<Throwable> undeliverableErrors = new CopyOnWriteArrayList<>();

  @BeforeEach
  public void before() {
    ApiServiceOption serviceOption =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.POST)
            .streamingMode(StreamingMode.OUT)
            .outputMode(OutputMode.ACCUMULATE)
            .taskGroup("group")
            .task("task")
            .function("function")
            .build();
    serviceOption.setIsSSE(true);
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
    undeliverableErrors.clear();
    RxJavaPlugins.setErrorHandler(undeliverableErrors::add);
  }

  @AfterEach
  public void after() {
    RxJavaPlugins.setErrorHandler(null);
  }

  private ApiException streamError(MockResponse response) throws IOException, NoApiKeyException {
    MockWebServer server = new MockWebServer();
    server.enqueue(response);
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", server.getPort());
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("pre-gateway-mock-async-test").build();
    Flowable<DashScopeResult> flowable = syncApi.streamCall(param);
    ApiException e = assertThrows(ApiException.class, () -> flowable.toList().blockingGet());
    server.close();
    return e;
  }

  private static MockResponse sseErrorEvent() {
    return new MockResponse()
        .setResponseCode(200)
        .setHeader("content-type", "text/event-stream")
        .setBody("event:error\ndata:" + ERROR_JSON + "\n\n");
  }

  private static String sseErrorEventWithPadding() {
    StringBuilder sb = new StringBuilder("event:error\ndata:" + ERROR_JSON + "\n\n");
    for (int i = 0; i < 4096; i++) {
      sb.append(": padding comment line\n");
    }
    return sb.toString();
  }

  @Test
  public void testSseErrorEventSurfacesBackendError() throws IOException, NoApiKeyException {
    ApiException e = streamError(sseErrorEvent());
    assertEquals("InvalidParameter", e.getStatus().getCode());
    assertTrue(e.getStatus().getMessage().contains("url error"));
    assertEquals("ffbb90bf-1b08-9bfc-9cb3-94eeee5a9258", e.getStatus().getRequestId());
  }

  @Test
  public void testBrokenStreamAfterErrorEventDeliversSingleError()
      throws IOException, InterruptedException, NoApiKeyException {
    ApiException e =
        streamError(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("content-type", "text/event-stream")
                .setBody(sseErrorEventWithPadding())
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
    // The backend error from the error event wins over the later connection failure.
    assertEquals("InvalidParameter", e.getStatus().getCode());
    assertTrue(e.getStatus().getMessage().contains("url error"));
    Thread.sleep(500);
    assertTrue(
        undeliverableErrors.isEmpty(),
        "expected no undeliverable errors, got: " + undeliverableErrors);
  }

  @Test
  public void testJsonErrorWithNonJsonContentType() throws IOException, NoApiKeyException {
    ApiException e =
        streamError(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("content-type", "text/plain")
                .setBody(ERROR_JSON));
    assertEquals("InvalidParameter", e.getStatus().getCode());
    assertTrue(e.getStatus().getMessage().contains("url error"));
    assertEquals("ffbb90bf-1b08-9bfc-9cb3-94eeee5a9258", e.getStatus().getRequestId());
  }

  @Test
  public void testRawJsonBodyWithEventStreamContentTypeOnHttpError()
      throws IOException, NoApiKeyException {
    ApiException e =
        streamError(
            new MockResponse()
                .setResponseCode(400)
                .setHeader("content-type", "text/event-stream")
                .setBody(ERROR_JSON));
    assertEquals("InvalidParameter", e.getStatus().getCode());
    assertTrue(e.getStatus().getMessage().contains("url error"));
    assertEquals("ffbb90bf-1b08-9bfc-9cb3-94eeee5a9258", e.getStatus().getRequestId());
  }

  @Test
  public void testBrokenStreamReportsIoMessage() throws IOException, NoApiKeyException {
    ApiException e =
        streamError(
            new MockResponse()
                .setResponseCode(200)
                .setHeader("content-type", "text/event-stream")
                .setBody("data:{\"output\":{\"text\":\"hi\"}}\n\n")
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
    assertEquals("response_error", e.getStatus().getCode());
    assertTrue(
        e.getStatus().getMessage() != null && !e.getStatus().getMessage().isEmpty(),
        "expected a non-empty message, got: " + e.getStatus().getMessage());
  }

  @Test
  public void testNormalStreamCompletion()
      throws IOException, InterruptedException, NoApiKeyException {
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("content-type", "text/event-stream")
            .setBody(
                "data:{\"output\":{\"text\":\"hello\"}}\n\ndata:{\"output\":{\"text\":\"world\"}}\n\n"));
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", server.getPort());
    HalfDuplexTestParam param = HalfDuplexTestParam.builder().model("qwen-turbo").build();
    List<DashScopeResult> results = syncApi.streamCall(param).toList().blockingGet();
    assertEquals(2, results.size());
    Thread.sleep(200);
    assertTrue(undeliverableErrors.isEmpty());
    server.close();
  }

  @Test
  public void testCallbackReceivesSingleOnError()
      throws IOException, InterruptedException, NoApiKeyException {
    MockWebServer server = new MockWebServer();
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("content-type", "text/event-stream")
            .setBody(sseErrorEventWithPadding())
            .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
    Constants.baseHttpApiUrl = String.format("http://127.0.0.1:%s", server.getPort());
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().model("pre-gateway-mock-async-test").build();
    AtomicInteger errorCount = new AtomicInteger(0);
    AtomicInteger completeCount = new AtomicInteger(0);
    Semaphore done = new Semaphore(0);
    List<Exception> errors = new CopyOnWriteArrayList<>();
    syncApi.streamCall(
        param,
        new ResultCallback<DashScopeResult>() {
          @Override
          public void onEvent(DashScopeResult message) {}

          @Override
          public void onComplete() {
            completeCount.incrementAndGet();
            done.release();
          }

          @Override
          public void onError(Exception e) {
            errorCount.incrementAndGet();
            errors.add(e);
            done.release();
          }
        });
    assertTrue(done.tryAcquire(10, TimeUnit.SECONDS), "callback not invoked in time");
    Thread.sleep(500);
    assertEquals(1, errorCount.get(), "onError should be invoked exactly once");
    assertEquals(0, completeCount.get(), "onComplete should not be invoked after onError");
    assertEquals("InvalidParameter", ((ApiException) errors.get(0)).getStatus().getCode());
    server.close();
  }
}
