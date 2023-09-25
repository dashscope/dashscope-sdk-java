// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol.okhttp;

import com.alibaba.dashscope.protocol.ClientOptions;
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
      ClientOptions defaultOptions = ClientOptions.builder().build();
      HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
      logging.setLevel(Level.valueOf(defaultOptions.getNetworkLoggingLevel()));

      Dispatcher dispatcher = new Dispatcher();
      if (dispatcher.getMaxRequests() < defaultOptions.getConnectionPoolSize()) {
        dispatcher.setMaxRequests(defaultOptions.getConnectionPoolSize());
        dispatcher.setMaxRequestsPerHost(defaultOptions.getConnectionPoolSize());
      }
      Builder clientBuilder = new OkHttpClient.Builder();
      clientBuilder
          .connectTimeout(defaultOptions.getConnectTimeout())
          .readTimeout(defaultOptions.getReadTimeout())
          .writeTimeout(defaultOptions.getWriteTimeout())
          .addInterceptor(logging)
          .dispatcher(dispatcher)
          .protocols(Collections.singletonList(Protocol.HTTP_1_1))
          .connectionPool(
              new ConnectionPool(
                  defaultOptions.getConnectionPoolSize(),
                  defaultOptions.getConnectionIdelTimeout().getSeconds(),
                  TimeUnit.SECONDS));
      return clientBuilder.build();
    }
  }

  public static OkHttpClient getOkHttpClient() {
    return Holder.INSTANCE;
  }

  public static OkHttpClient getNewOkHttpClient(ClientOptions options) {
    Builder builder = Holder.INSTANCE.newBuilder();
    builder
        .connectTimeout(options.getConnectTimeout())
        .readTimeout(options.getReadTimeout())
        .writeTimeout(options.getWriteTimeout());
    return builder.build();
  }
}
