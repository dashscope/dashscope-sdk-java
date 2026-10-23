// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.ConnectionConfigurations;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

public class TestConnectionOptions {

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_CONNECTION_POOL_SIZE", value = "100")
  @SetEnvironmentVariable(key = "DASHSCOPE_MAXIMUM_ASYNC_REQUESTS", value = "101")
  @SetEnvironmentVariable(key = "DASHSCOPE_MAXIMUM_ASYNC_REQUESTS_PER_HOST", value = "102")
  public void testSetConnectionPoolWithEnv() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertEquals(clConnectionOptions.getConnectionPoolSize(), 100);
    assertEquals(clConnectionOptions.getMaximumAsyncRequests(), 101);
    assertEquals(clConnectionOptions.getMaximumAsyncRequestsPerHost(), 102);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_CONNECTION_IDLE_TIME", value = "101")
  public void testSetConnectionIdelTimeWithEnv() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertEquals(clConnectionOptions.getConnectionIdleTimeout(), Duration.ofSeconds(101));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WRITE_TIMEOUT", value = "101")
  public void testSetConnectionWriteTimeWithEnv() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertEquals(clConnectionOptions.getWriteTimeout(), Duration.ofSeconds(101));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_READ_TIMEOUT", value = "101")
  public void testSetConnectionReadTimeWithEnv() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertEquals(clConnectionOptions.getReadTimeout(), Duration.ofSeconds(101));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_CONNECTION_TIMEOUT", value = "101")
  public void testSetConnectionTimeWithEnv() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertEquals(clConnectionOptions.getConnectTimeout(), Duration.ofSeconds(101));
  }

  @Test
  public void testGetDefaultValues() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertEquals(clConnectionOptions.getConnectionPoolSize(), 32);
    assertEquals(clConnectionOptions.getMaximumAsyncRequests(), 32);
    assertEquals(clConnectionOptions.getMaximumAsyncRequestsPerHost(), 32);
    assertEquals(clConnectionOptions.getConnectTimeout(), Duration.ofSeconds(120));
    assertEquals(clConnectionOptions.getWriteTimeout(), Duration.ofSeconds(60));
    assertEquals(clConnectionOptions.getReadTimeout(), Duration.ofSeconds(300));
    assertEquals(clConnectionOptions.getConnectionIdleTimeout(), Duration.ofSeconds(300));
  }

  @Test
  public void testSetValues() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions =
        ConnectionConfigurations.builder()
            .connectTimeout(Duration.ofSeconds(1))
            .connectionIdleTimeout(Duration.ofSeconds(2))
            .connectionPoolSize(15)
            .writeTimeout(Duration.ofSeconds(3))
            .readTimeout(Duration.ofSeconds(4))
            .build();
    assertEquals(clConnectionOptions.getConnectionPoolSize(), 15);
    assertEquals(clConnectionOptions.getConnectTimeout(), Duration.ofSeconds(1));
    assertEquals(clConnectionOptions.getWriteTimeout(), Duration.ofSeconds(3));
    assertEquals(clConnectionOptions.getReadTimeout(), Duration.ofSeconds(4));
    assertEquals(clConnectionOptions.getConnectionIdleTimeout(), Duration.ofSeconds(2));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_PROXY_HOST", value = "https://host.com")
  @SetEnvironmentVariable(key = "DASHSCOPE_PROXY_PORT", value = "9887")
  public void testSetProxyWithEnv() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    Proxy proxy = clConnectionOptions.getProxy();
    assertEquals(proxy.type(), Proxy.Type.HTTP);
    InetSocketAddress addr = (InetSocketAddress) proxy.address();
    assertEquals(addr.getHostName(), "https://host.com");
    assertEquals(addr.getPort(), 9887);
  }

  @Test
  public void testDefaultProxy() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions = ConnectionConfigurations.builder().build();
    assertNull(clConnectionOptions.getProxy());
  }

  @Test
  public void testSetProxy() throws NoApiKeyException {
    ConnectionConfigurations clConnectionOptions =
        ConnectionConfigurations.builder().proxyHost("https://host.com").proxyPort(9887).build();
    Proxy proxy = clConnectionOptions.getProxy();
    assertEquals(proxy.type(), Proxy.Type.HTTP);
    InetSocketAddress addr = (InetSocketAddress) proxy.address();
    assertEquals(addr.getHostName(), "https://host.com");
    assertEquals(addr.getPort(), 9887);
  }

  @Test
  public void testAuthenticator() {
    Authenticator proxyAuthenticator =
        new Authenticator() {
          @Override
          public Request authenticate(Route route, Response response) throws IOException {
            String credential = Credentials.basic("user", "123");
            return response
                .request()
                .newBuilder()
                .header("Proxy-Authorization", credential)
                .build();
          }
        };
    ConnectionConfigurations connectionOptions =
        ConnectionConfigurations.builder().proxyAuthenticator(proxyAuthenticator).build();
    assertNotNull(connectionOptions.getProxyAuthenticator());
  }
}
