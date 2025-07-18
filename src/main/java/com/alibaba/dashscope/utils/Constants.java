// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.protocol.ConnectionConfigurations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class Constants {

  public static final String NO_API_KEY_ERROR = "Can not find api-key.";

  public static final String DASHSCOPE_API_REGION_ENV = "DASHSCOPE_API_REGION";

  public static final String DASHSCOPE_API_VERSION_ENV = "DASHSCOPE_API_VERSION";

  public static final String MAX_CONNECTIONS_HTTP = "MAX_CONNECTIONS_HTTP";

  public static final String MAX_CONNECTIONS_PER_ROUTE_HTTP = "MAX_CONNECTIONS_PER_ROUTE_HTTP";

  public static final String HTTP_CONNECT_TIMEOUT_ENV = "HTTP_CONNECT_TIMEOUT";

  public static final String HTTP_CONNECTION_REQUEST_TIMEOUT_ENV =
      "HTTP_CONNECTION_REQUEST_TIMEOUT";

  public static final String DASHSCOPE_API_KEY_ENV = "DASHSCOPE_API_KEY";

  public static final String DASHSCOPE_API_KEY_FILE_PATH_ENV = "DASHSCOPE_API_KEY_FILE_PATH";

  public static final String DASHSCOPE_HTTP_BASE_URL_ENV = "DASHSCOPE_HTTP_BASE_URL";

  public static final String DASHSCOPE_WEBSOCKET_BASE_URL_ENV = "DASHSCOPE_WEBSOCKET_BASE_URL";
  public static final String DASHSCOPE_WEBSOCKET_OMNI_BASE_URL_ENV =
          "DASHSCOPE_WEBSOCKET_OMNI_BASE_URL";
  public static final String DASHSCOPE_WEBSOCKET_QWEN_TTS_REALTIME_BASE_URL_ENV =
          "DASHSCOPE_WEBSOCKET_QWEN_TTS_REALTIME_BASE_URL";
  // Setting network layer logging, support: [NONE, BASIC, HEADERS, BODY]
  public static final String DASHSCOPE_NETWORK_LOGGING_LEVEL_ENV =
      "DASHSCOPE_NETWORK_LOGGING_LEVEL";
  public static final String DASHSCOPE_SDK_LOGGING_LEVEL_ENV = "DASHSCOPE_SDK_LOGGING_LEVEL";
  public static final String DASHSCOPE_CONNECTION_POOL_SIZE_ENV = "DASHSCOPE_CONNECTION_POOL_SIZE";
  public static final String DASHSCOPE_CONNECTION_IDLE_TIMEOUT_ENV =
      "DASHSCOPE_CONNECTION_IDLE_TIME";
  public static final String DASHSCOPE_WRITE_TIMEOUT_ENV = "DASHSCOPE_WRITE_TIMEOUT";
  public static final String DASHSCOPE_READ_TIMEOUT_ENV = "DASHSCOPE_READ_TIMEOUT";
  public static final String DASHSCOPE_CONNECTION_TIMEOUT_ENV = "DASHSCOPE_CONNECTION_TIMEOUT";

  public static final int DASHSCOPE_WEBSOCKET_FAILED_STATUS_CODE = 44;

  public static int CONNECT_TIMEOUT =
      Integer.parseInt(System.getenv().getOrDefault(HTTP_CONNECT_TIMEOUT_ENV, "10000"));

  public static int CONNECTION_REQUEST_TIMEOUT =
      Integer.parseInt(System.getenv().getOrDefault(HTTP_CONNECTION_REQUEST_TIMEOUT_ENV, "30000"));

  public static String apiVersion = System.getenv().getOrDefault(DASHSCOPE_API_VERSION_ENV, "v1");

  public static String apiRegion =
      System.getenv().getOrDefault(DASHSCOPE_API_REGION_ENV, "cn-beijing");

  public static String apiKey = null;

  public static int max_connections_http =
      Integer.parseInt(System.getenv().getOrDefault(MAX_CONNECTIONS_HTTP, "100"));

  public static int max_connections_per_route_http =
      Integer.parseInt(System.getenv().getOrDefault(MAX_CONNECTIONS_PER_ROUTE_HTTP, "20"));

  public static String apiKeyFilePath = null;

  public static String baseHttpApiUrl =
      System.getenv()
          .getOrDefault(
              DASHSCOPE_HTTP_BASE_URL_ENV, "https://dashscope.aliyuncs.com/api/" + apiVersion);

  public static String baseWebsocketApiUrl =
      System.getenv()
          .getOrDefault(
              DASHSCOPE_WEBSOCKET_BASE_URL_ENV,
              String.format("wss://dashscope.aliyuncs.com/api-ws/%s/inference/", apiVersion));
  public static ConnectionConfigurations connectionConfigurations = null;

  public static final int MAX_PROMPT_LENGTH = 500;

  public static void init() {}

  static {
    init();
  }
}
