// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.utils;

import com.alibaba.dashscope.resources.agentstudio.support.exception.NoApiKeyException;
import com.alibaba.dashscope.shared.EnvNames;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 3.0 copy of the 2.x ApiKey resolution chain for the agentstudio subtree. The user-settable
 * global slot stays single-sourced on {@code com.alibaba.dashscope.utils.Constants#apiKey} so 2.x
 * callers who set it keep working unchanged (design §7 zero-impact constraint).
 */
public final class ApiKey {
  public static String getApiKey(String apiKey) throws NoApiKeyException {
    if (apiKey != null) { // api specified
      return apiKey;
    }
    if (com.alibaba.dashscope.utils.Constants.apiKey != null) { // Constants.apiKey=xxx
      return com.alibaba.dashscope.utils.Constants.apiKey;
    }
    String value = System.getenv(EnvNames.DASHSCOPE_API_KEY);
    if (value != null) { // from environment variable.
      return value;
    }
    String apiKeyFilePath = System.getenv(EnvNames.DASHSCOPE_API_KEY_FILE_PATH);
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
