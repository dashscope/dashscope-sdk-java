// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.protocol.pool;

import com.alibaba.dashscope.utils.Constants;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;

public class HttpPool {

  private static volatile HttpPool instance;

  private final PoolingNHttpClientConnectionManager connectionManager;

  private final CloseableHttpAsyncClient httpClient;

  private final ScheduledExecutorService executorService;

  private HttpPool() throws IOReactorException {
    connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
    connectionManager.setMaxTotal(Constants.max_connections_http);
    connectionManager.setDefaultMaxPerRoute(Constants.max_connections_per_route_http);
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(Constants.CONNECT_TIMEOUT)
            .setConnectionRequestTimeout(Constants.CONNECTION_REQUEST_TIMEOUT)
            .setSocketTimeout(Constants.SOCKET_TIMEOUT)
            .build();
    httpClient =
        HttpAsyncClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
    httpClient.start();

    executorService =
        new ScheduledThreadPoolExecutor(
            1,
            new BasicThreadFactory.Builder()
                .namingPattern("http-schedule-pool-%d")
                .daemon(true)
                .build());
    executorService.scheduleAtFixedRate(
        () -> {
          try {
            connectionManager.closeExpiredConnections();
            connectionManager.closeIdleConnections(10, TimeUnit.SECONDS);
          } catch (Throwable t) {
            t.printStackTrace();
          }
        },
        0,
        10,
        TimeUnit.SECONDS);
  }

  public static HttpPool getInstance() throws IOReactorException {
    if (instance == null) {
      synchronized (HttpPool.class) {
        if (instance == null) {
          instance = new HttpPool();
        }
      }
    }
    return instance;
  }

  public CloseableHttpAsyncClient getHttpClient() {
    return httpClient;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    if (executorService != null) {
      executorService.shutdown();
    }
    if (httpClient != null) {
      httpClient.close();
    }
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
  }
}
