package com.dashscope.protocol;

import com.dashscope.utils.Constants;
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

public class HttpFactory {

  private static volatile HttpFactory instance;

  private final PoolingNHttpClientConnectionManager connectionManager;

  private final CloseableHttpAsyncClient httpClient;

  private final ScheduledExecutorService executorService;

  private static final int CONNECT_TIMEOUT = 3000;

  private static final int CONNECTION_REQUEST_TIMEOUT = 3000;

  private static final int SOCKET_TIMEOUT = 3000;

  private HttpFactory() throws IOReactorException {
    connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
    connectionManager.setMaxTotal(Constants.max_connections_http);
    connectionManager.setDefaultMaxPerRoute(Constants.max_connections_per_route_http);
    RequestConfig requestConfig =
        RequestConfig.custom()
            .setConnectTimeout(CONNECT_TIMEOUT)
            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
            .setSocketTimeout(SOCKET_TIMEOUT)
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

  public static HttpFactory getInstance() throws IOReactorException {
    if (instance == null) {
      synchronized (HttpFactory.class) {
        if (instance == null) {
          instance = new HttpFactory();
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
