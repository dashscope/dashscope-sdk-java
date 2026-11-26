// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import com.alibaba.dashscope.applications.agentstudio.exception.NoApiKeyException;
import com.alibaba.dashscope.shared.EnvNames;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

/**
 * AgentStudio-owned API-key resolver.
 *
 * <p>Resolution order: explicit key, {@code DASHSCOPE_API_KEY} environment variable, {@code
 * DASHSCOPE_API_KEY_FILE_PATH} file, then the default {@code ~/.dashscope/api_key} file. Unlike the
 * former shared utility there is no mutable global fallback key.
 */
@Slf4j
public final class ApiKey {
  public static String getApiKey(String apiKey) throws NoApiKeyException {
    if (apiKey != null) { // api specified
      return apiKey;
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
