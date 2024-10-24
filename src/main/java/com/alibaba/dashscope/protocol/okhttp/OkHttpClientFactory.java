// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.protocol.ClientOptions;
import com.alibaba.dashscope.protocol.ConnectionConfigurations;
import com.alibaba.dashscope.protocol.ConnectionOptions;
import com.alibaba.dashscope.utils.Constants;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

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
      ConnectionConfigurations connectionConfigurations = Constants.connectionConfigurations;
      if (connectionConfigurations == null) {
        // build default configuration
        connectionConfigurations = ConnectionConfigurations.builder().build();
      }

      ClientOptions defaultOptions = ClientOptions.builder().build();
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(Level.valueOf(defaultOptions.getNetworkLoggingLevel()));
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

  public static OkHttpClient getOkHttpClient() {
    /** Use default http client pool. */
    return Holder.INSTANCE;
  }

  public static OkHttpClient getNewOkHttpClient(ConnectionOptions connectionOptions) {
    return Holder.INSTANCE
        .newBuilder()
        .connectTimeout(connectionOptions.getConnectTimeout())
        .readTimeout(connectionOptions.getReadTimeout())
        .writeTimeout(connectionOptions.getWriteTimeout())
        .build();
  }
}
