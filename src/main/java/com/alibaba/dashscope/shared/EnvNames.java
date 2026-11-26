// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.shared;

/**
 * Environment variable names shared by 2.x and 3.0 (design J6). Neutral leaf: JDK-only, no logic,
 * so both generations can reference it without a dependency cycle. All fields are compile-time
 * constants, keeping 2.x {@code Constants} fields constant-foldable.
 */
public final class EnvNames {
  private EnvNames() {}

  // ---- shared by 2.x (utils/Constants) and 3.0 ----
  public static final String DASHSCOPE_API_KEY = "DASHSCOPE_API_KEY";
  public static final String DASHSCOPE_API_KEY_FILE_PATH = "DASHSCOPE_API_KEY_FILE_PATH";
  public static final String DASHSCOPE_HTTP_BASE_URL = "DASHSCOPE_HTTP_BASE_URL";
  public static final String DASHSCOPE_WEBSOCKET_BASE_URL = "DASHSCOPE_WEBSOCKET_BASE_URL";
  public static final String DASHSCOPE_API_REGION = "DASHSCOPE_API_REGION";
  public static final String DASHSCOPE_API_VERSION = "DASHSCOPE_API_VERSION";
  public static final String MAX_CONNECTIONS_HTTP = "MAX_CONNECTIONS_HTTP";
  public static final String MAX_CONNECTIONS_PER_ROUTE_HTTP = "MAX_CONNECTIONS_PER_ROUTE_HTTP";
  public static final String HTTP_CONNECT_TIMEOUT = "HTTP_CONNECT_TIMEOUT";
  public static final String HTTP_CONNECTION_REQUEST_TIMEOUT = "HTTP_CONNECTION_REQUEST_TIMEOUT";
  public static final String DASHSCOPE_WEBSOCKET_OMNI_BASE_URL =
      "DASHSCOPE_WEBSOCKET_OMNI_BASE_URL";
  public static final String DASHSCOPE_WEBSOCKET_QWEN_TTS_REALTIME_BASE_URL =
      "DASHSCOPE_WEBSOCKET_QWEN_TTS_REALTIME_BASE_URL";
  public static final String DASHSCOPE_WEBSOCKET_QWEN_LIVE_BASE_URL =
      "DASHSCOPE_WEBSOCKET_QWEN_LIVE_BASE_URL";
  public static final String DASHSCOPE_NETWORK_LOGGING_LEVEL = "DASHSCOPE_NETWORK_LOGGING_LEVEL";
  public static final String DASHSCOPE_SDK_LOGGING_LEVEL = "DASHSCOPE_SDK_LOGGING_LEVEL";
  public static final String DASHSCOPE_DISABLE_SDK_HEADERS = "DASHSCOPE_DISABLE_SDK_HEADERS";
  public static final String DASHSCOPE_CONNECTION_POOL_SIZE = "DASHSCOPE_CONNECTION_POOL_SIZE";
  public static final String DASHSCOPE_CONNECTION_IDLE_TIME = "DASHSCOPE_CONNECTION_IDLE_TIME";
  public static final String DASHSCOPE_WRITE_TIMEOUT = "DASHSCOPE_WRITE_TIMEOUT";
  public static final String DASHSCOPE_READ_TIMEOUT = "DASHSCOPE_READ_TIMEOUT";
  public static final String DASHSCOPE_CONNECTION_TIMEOUT = "DASHSCOPE_CONNECTION_TIMEOUT";

  // ---- 3.0 only ----
  public static final String DASHSCOPE_BASE_URL = "DASHSCOPE_BASE_URL";
  public static final String DASHSCOPE_WORKSPACE = "DASHSCOPE_WORKSPACE";
  public static final String DASHSCOPE_REGION = "DASHSCOPE_REGION";
  public static final String DASHSCOPE_TIMEOUT = "DASHSCOPE_TIMEOUT";
  public static final String DASHSCOPE_MAX_RETRIES = "DASHSCOPE_MAX_RETRIES";
  public static final String DASHSCOPE_WS_PING_INTERVAL = "DASHSCOPE_WS_PING_INTERVAL";
  public static final String DASHSCOPE_CONFIG = "DASHSCOPE_CONFIG";
  public static final String DASHSCOPE_HTTP_MAX_CONNECTIONS = "DASHSCOPE_HTTP_MAX_CONNECTIONS";
  public static final String DASHSCOPE_HTTP_MAX_REQUESTS_PER_HOST =
      "DASHSCOPE_HTTP_MAX_REQUESTS_PER_HOST";
  public static final String DASHSCOPE_HTTP_MAX_KEEPALIVE_CONNECTIONS =
      "DASHSCOPE_HTTP_MAX_KEEPALIVE_CONNECTIONS";
  public static final String DASHSCOPE_HTTP_KEEPALIVE_EXPIRY = "DASHSCOPE_HTTP_KEEPALIVE_EXPIRY";
}
