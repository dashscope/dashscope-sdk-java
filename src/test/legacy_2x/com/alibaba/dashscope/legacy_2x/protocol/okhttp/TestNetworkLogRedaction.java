// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.protocol.okhttp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * HEADERS/BODY network logging must never print credentials: the factory interceptor redacts
 * Authorization and X-DashScope-EncryptionKey. Records are captured from the JUL logger OkHttp's
 * default HttpLoggingInterceptor logger writes to (okhttp3.OkHttpClient, INFO).
 */
@Execution(ExecutionMode.SAME_THREAD)
public class TestNetworkLogRedaction {
  private MockWebServer server;
  private final List<String> lines = new ArrayList<>();
  private Logger julLogger;
  private Handler capture;
  private Level originalLevel;

  @BeforeEach
  public void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    capture =
        new Handler() {
          @Override
          public void publish(LogRecord record) {
            lines.add(record.getMessage());
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    julLogger = Logger.getLogger("okhttp3.OkHttpClient");
    originalLevel = julLogger.getLevel();
    julLogger.setLevel(Level.INFO);
    julLogger.setUseParentHandlers(false);
    julLogger.addHandler(capture);
  }

  @AfterEach
  public void tearDown() throws IOException {
    julLogger.removeHandler(capture);
    julLogger.setLevel(originalLevel);
    julLogger.setUseParentHandlers(true);
    server.shutdown();
  }

  private void executeOnce() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(OkHttpClientFactory.networkLoggingInterceptor())
            .build();
    Request request =
        new Request.Builder()
            .url(server.url("/"))
            .header("Authorization", "Bearer sk-secret-key")
            .header("X-DashScope-EncryptionKey", "enc-key-secret")
            .header("Cookie", "session=cookie-secret")
            .header("Proxy-Authorization", "Basic proxy-secret")
            .build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
    }
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "HEADERS")
  public void headersLevelRedactsCredentialHeaders() throws IOException {
    executeOnce();

    String log = String.join("\n", lines);
    assertTrue(log.contains("Authorization: ██"));
    assertTrue(log.contains("X-DashScope-EncryptionKey: ██"));
    assertTrue(log.contains("Cookie: ██"));
    assertTrue(log.contains("Proxy-Authorization: ██"));
    assertFalse(log.contains("sk-secret-key"));
    assertFalse(log.contains("enc-key-secret"));
    assertFalse(log.contains("cookie-secret"));
    assertFalse(log.contains("proxy-secret"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL", value = "BODY")
  public void bodyLevelDowngradesToHeadersForSse() throws IOException {
    // BODY buffers the whole response before releasing it, which would stall SSE streams; the
    // guard logs them at HEADERS, so the payload must not reach the log while the body stays
    // intact for the caller.
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "text/event-stream")
            .setBody("data: sse-body-marker\n\n"));
    OkHttpClient client =
        new OkHttpClient.Builder()
            .addInterceptor(OkHttpClientFactory.networkLoggingInterceptor())
            .build();
    Request request =
        new Request.Builder().url(server.url("/")).header("Accept", "text/event-stream").build();
    try (Response response = client.newCall(request).execute()) {
      assertEquals(200, response.code());
      assertEquals("data: sse-body-marker\n\n", response.body().string());
    }

    String log = String.join("\n", lines);
    assertTrue(log.contains("Accept: text/event-stream"));
    assertFalse(log.contains("sse-body-marker"));
  }

  @Test
  @ClearEnvironmentVariable(key = "DASHSCOPE_NETWORK_LOGGING_LEVEL")
  public void defaultNoneStaysSilent() throws IOException {
    executeOnce();

    String url = server.url("/").toString();
    assertTrue(lines.stream().noneMatch(l -> l.contains(url)));
  }
}
