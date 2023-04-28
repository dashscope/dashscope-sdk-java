package com.dashscope.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Constants {

  public static final String DASHSCOPE_API_REGION_ENV = "DASHSCOPE_API_REGION";

  public static final String DASHSCOPE_API_VERSION_ENV = "DASHSCOPE_API_VERSION";

  public static final String MAX_CONNECTIONS_HTTP = "MAX_CONNECTIONS_HTTP";

  public static final String MAX_CONNECTIONS_PER_ROUTE_HTTP = "MAX_CONNECTIONS_PER_ROUTE_HTTP";

  public static final String DASHSCOPE_API_KEY_ENV = "DASHSCOPE_API_KEY";

  public static final String DASHSCOPE_API_KEY_FILE_PATH_ENV = "DASHSCOPE_API_KEY_FILE_PATH";

  public static final String DASHSCOPE_HTTP_BASE_URL_ENV = "DASHSCOPE_HTTP_BASE_URL";

  public static final String DASHSCOPE_WEBSOCKET_BASE_URL_ENV = "DASHSCOPE_WEBSOCKET_BASE_URL";

  public static String apiVersion = System.getenv().getOrDefault(DASHSCOPE_API_VERSION_ENV, "v1");

  public static String apiRegion =
      System.getenv().getOrDefault(DASHSCOPE_API_REGION_ENV, "cn-beijing");

  public static String apiKey = System.getenv(DASHSCOPE_API_KEY_ENV);

  public static int max_connections_http =
      Integer.parseInt(System.getenv().getOrDefault(MAX_CONNECTIONS_HTTP, "100"));

  public static int max_connections_per_route_http =
      Integer.parseInt(System.getenv().getOrDefault(MAX_CONNECTIONS_PER_ROUTE_HTTP, "20"));

  public static String apiKeyFilePath = System.getenv(DASHSCOPE_API_KEY_FILE_PATH_ENV);

  public static String baseHttpApiUrl =
      System.getenv()
          .getOrDefault(
              DASHSCOPE_HTTP_BASE_URL_ENV, "https://dashscope.aliyuncs.com/api/" + apiVersion);

  public static String baseWebsocketApiUrl =
      System.getenv()
          .getOrDefault(
              DASHSCOPE_WEBSOCKET_BASE_URL_ENV,
              String.format("wss://dashscope.aliyuncs.com/api-ws/%s/inference/", apiVersion));

  static {
    if (apiKey == null) {
      String apiFile = apiKeyFilePath;
      if (apiFile == null) {
        Path homePath = Paths.get(System.getProperty("user.home"));
        Path dashscopePath = homePath.resolve(".dashscope").resolve("api_key");
        apiFile = dashscopePath.toString();
      }
      try (BufferedReader reader = new BufferedReader(new FileReader(apiFile))) {
        apiKey = reader.readLine().trim();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
