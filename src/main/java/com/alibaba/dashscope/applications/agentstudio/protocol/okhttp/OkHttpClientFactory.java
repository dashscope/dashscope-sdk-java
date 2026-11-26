// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol.okhttp;

import com.alibaba.dashscope.applications.agentstudio.protocol.ClientOptions;
import com.alibaba.dashscope.applications.agentstudio.protocol.ConnectionConfigurations;
import com.alibaba.dashscope.applications.agentstudio.protocol.ConnectionOptions;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/** Builds the OkHttp clients used by the AgentStudio HTTP transport. */
@Slf4j
public class OkHttpClientFactory {
  private OkHttpClientFactory() {
    if (Holder.INSTANCE != null) {
      throw new IllegalStateException();
    }
  }

  private static class Holder {
    private static final OkHttpClient INSTANCE = createInstance();

    private static OkHttpClient createInstance() {
      // AgentStudio owns its default configuration; no mutable global override.
      ConnectionConfigurations connectionConfigurations =
          ConnectionConfigurations.builder().build();

      Interceptor logging = networkLoggingInterceptor();
      int connectionPoolSize = connectionConfigurations.getConnectionPoolSize();
      log.debug("[connectionPool Config] connectionPoolSize: {}", connectionPoolSize);
      Dispatcher dispatcher = new Dispatcher();
      dispatcher.setMaxRequests(connectionConfigurations.getMaximumAsyncRequests());
      log.debug("[connectionPool Config] maxRequests: {}", dispatcher.getMaxRequests());
      dispatcher.setMaxRequestsPerHost(connectionConfigurations.getMaximumAsyncRequestsPerHost());
      log.debug(
          "[connectionPool Config] maxRequestsPerHost: {}", dispatcher.getMaxRequestsPerHost());
      Builder clientBuilder = new OkHttpClient.Builder();
      clientBuilder
          .connectTimeout(connectionConfigurations.getConnectTimeout())
          .readTimeout(connectionConfigurations.getReadTimeout())
          .writeTimeout(connectionConfigurations.getWriteTimeout())
          .addInterceptor(logging)
          .dispatcher(dispatcher)
          .protocols(Collections.singletonList(Protocol.HTTP_1_1))
          .connectionPool(
              new ConnectionPool(
                  connectionPoolSize,
                  connectionConfigurations.getConnectionIdleTimeout().getSeconds(),
                  TimeUnit.SECONDS));
      if (connectionConfigurations.getProxy() != null) {
        clientBuilder.proxy(connectionConfigurations.getProxy());
      }
      if (connectionConfigurations.getProxyAuthenticator() != null) {
        clientBuilder.proxyAuthenticator(connectionConfigurations.getProxyAuthenticator());
      }
      return clientBuilder.build();
    }
  }

  /**
   * Network logging driven by DASHSCOPE_NETWORK_LOGGING_LEVEL (default NONE). Credential headers
   * are redacted so HEADERS/BODY debugging never prints the API key. Package-private for tests.
   */
  static Interceptor networkLoggingInterceptor() {
    ClientOptions defaultOptions = ClientOptions.builder().build();
    Level level = Level.valueOf(defaultOptions.getNetworkLoggingLevel());
    HttpLoggingInterceptor full = redactingLogging(level);
    if (level != Level.BODY) {
      return full;
    }
    // BODY buffers the whole response before releasing it, which would stall SSE streams until
    // they end; log those at HEADERS instead. Caller headers may override Accept, so also match
    // the SDK's own X-DashScope-SSE marker.
    HttpLoggingInterceptor headersOnly = redactingLogging(Level.HEADERS);
    return chain ->
        ("text/event-stream".equalsIgnoreCase(chain.request().header("Accept"))
                    || "enable".equalsIgnoreCase(chain.request().header("X-DashScope-SSE"))
                ? headersOnly
                : full)
            .intercept(chain);
  }

  private static HttpLoggingInterceptor redactingLogging(Level level) {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(level);
    logging.redactHeader("Authorization");
    logging.redactHeader("X-DashScope-EncryptionKey");
    logging.redactHeader("Cookie");
    logging.redactHeader("Set-Cookie");
    logging.redactHeader("Proxy-Authorization");
    return logging;
  }

  public static OkHttpClient getOkHttpClient() {
    /** Use default http client pool. */
    return Holder.INSTANCE;
  }

  public static OkHttpClient getNewOkHttpClient(ConnectionOptions connectionOptions) {
    Builder builder = Holder.INSTANCE.newBuilder();

    if (connectionOptions.hasCustomConnectTimeout()) {
      builder.connectTimeout(connectionOptions.getConnectTimeout());
    }
    if (connectionOptions.hasCustomReadTimeout()) {
      builder.readTimeout(connectionOptions.getReadTimeout());
    }
    if (connectionOptions.hasCustomWriteTimeout()) {
      builder.writeTimeout(connectionOptions.getWriteTimeout());
    }

    // Configure proxy if available
    if (connectionOptions.getProxy() != null) {
      builder.proxy(connectionOptions.getProxy());
    }

    // Configure proxy authenticator if available
    if (connectionOptions.getProxyAuthenticator() != null) {
      builder.proxyAuthenticator(connectionOptions.getProxyAuthenticator());
    }

    return builder.build();
  }
}
