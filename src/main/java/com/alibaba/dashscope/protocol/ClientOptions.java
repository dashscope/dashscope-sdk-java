// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.utils.Constants;
import java.time.Duration;
import java.util.Arrays;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class ClientOptions {
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(120);
  private static final Duration DEFAULT_WRITE_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);
  private static final Duration DEFAULT_CONNECTION_IDLE_TIMEOUT = Duration.ofSeconds(300);
  private static final Integer DEFAULT_CONNECTION_POOL_SIZE = 5;
  private static final String DEFAULT_IMPLEMENTATION = "okhttp";
  private static final String DEFAULT_PROTOCOL = "https";
  // enable detail log of the communication.
  // [NONE, BASIC, HEADERS, BODY]
  private static final String DEFAULT_NETWORK_LOGGING_LEVEL = "NONE";
  private static final String DEFAULT_SDK_LOGGING_LEVEL = "WARN";
  // websocket streaming mode.
  private static final String DEFAULT_STREAMING_MODE = "none";

  private Duration connectTimeout;
  private Duration writeTimeout;
  private Duration responseTimeout;
  private Duration readTimeout;
  private Integer connectionPoolSize;
  private Duration connectionIdleTimeout;
  private String implementation;
  private String protocol;
  private String networkLoggingLevel;
  private String sdkLoggingLevel;
  private String webSocketEndpoint;

  public String getWebSocketEndpoint() {
    if (webSocketEndpoint == null) {
      return Constants.baseWebsocketApiUrl;
    } else {
      return webSocketEndpoint;
    }
  }

  public Duration getConnectTimeout() {
    return getDuration(
        connectTimeout, DEFAULT_CONNECT_TIMEOUT, Constants.DASHSCOPE_CONNECTION_TIMEOUT_ENV);
  }

  public Duration getWriteTimeout() {
    return getDuration(writeTimeout, DEFAULT_WRITE_TIMEOUT, Constants.DASHSCOPE_WRITE_TIMEOUT_ENV);
  }

  public Duration getReadTimeout() {
    return getDuration(readTimeout, DEFAULT_READ_TIMEOUT, Constants.DASHSCOPE_READ_TIMEOUT_ENV);
  }

  public Duration getConnectionIdelTimeout() {
    return getDuration(
        connectionIdleTimeout,
        DEFAULT_CONNECTION_IDLE_TIMEOUT,
        Constants.DASHSCOPE_CONNECTION_IDLE_TIMEOUT_ENV);
  }

  public Integer getConnectionPoolSize() {
    try {
      Integer n = Integer.parseInt(System.getenv(Constants.DASHSCOPE_CONNECTION_POOL_SIZE_ENV));
      return n;
    } catch (NumberFormatException e) {
      return DEFAULT_CONNECTION_POOL_SIZE;
    }
  }

  public String getImplementation() {
    if (implementation != null) {
      return implementation;
    } else {
      return DEFAULT_IMPLEMENTATION;
    }
  }

  public String getProtocol() {
    if (protocol != null) {
      return protocol;
    }
    {
      return DEFAULT_PROTOCOL;
    }
  }

  public String getNetworkLoggingLevel() {
    if (networkLoggingLevel != null) {
      return networkLoggingLevel;
    } else {
      String logLevel =
          System.getenv()
              .getOrDefault(
                  Constants.DASHSCOPE_NETWORK_LOGGING_LEVEL_ENV, DEFAULT_NETWORK_LOGGING_LEVEL);
      if (Arrays.asList("NONE", "BASIC", "HEADERS", "BODY").contains(logLevel)) {
        return logLevel;
      } else {
        return DEFAULT_NETWORK_LOGGING_LEVEL;
      }
    }
  }

  public String getSdkLoggingLevel() {
    if (sdkLoggingLevel != null) {
      return sdkLoggingLevel;
    } else {
      return System.getenv()
          .getOrDefault(Constants.DASHSCOPE_SDK_LOGGING_LEVEL_ENV, DEFAULT_SDK_LOGGING_LEVEL);
    }
  }

  private Duration getDuration(Duration target, Duration defaultValue, String env) {
    if (target == null) {
      try {
        long dur = Integer.parseInt(System.getenv(env));
        return Duration.ofSeconds(dur);
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    } else {
      return target;
    }
  }
}
