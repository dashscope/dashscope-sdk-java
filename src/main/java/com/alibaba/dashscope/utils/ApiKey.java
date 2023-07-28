// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.exception.NoApiKeyException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ApiKey {
  public static String getApiKey(String apiKey) throws NoApiKeyException {
    if (apiKey != null) { // api specified
      return apiKey;
    }
    if (Constants.apiKey != null) { // Constants.apiKey=xxx
      return Constants.apiKey;
    }
    String value = System.getenv(Constants.DASHSCOPE_API_KEY_ENV);
    if (value != null) { // from environment variable.
      return value;
    }
    String apiKeyFilePath = System.getenv(Constants.DASHSCOPE_API_KEY_FILE_PATH_ENV);
    if (apiKeyFilePath == null || Files.notExists(Paths.get(apiKeyFilePath))) {
      // no api key file specified, check default api key file.
      Path homePath = Paths.get(System.getProperty("user.home"));
      Path defaultApiKeyPath = homePath.resolve(".dashscope").resolve("api_key");
      if (Files.notExists(defaultApiKeyPath)) {
        apiKeyFilePath = null;
      } else {
        try {
          BufferedReader reader = new BufferedReader(new FileReader(defaultApiKeyPath.toString()));
          apiKey = reader.readLine().trim();
          reader.close();
          return apiKey;
        } catch (Exception e) {
          throw new NoApiKeyException();
        }
      }
    }
    if (apiKeyFilePath != null) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(apiKeyFilePath));
        apiKey = reader.readLine().trim();
        reader.close();
        return apiKey;
      } catch (Exception e) {
        throw new NoApiKeyException();
      }
    }
    throw new NoApiKeyException();
  }
}
