// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.ApiKey;
import com.alibaba.dashscope.utils.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
@ClearEnvironmentVariable(key = "DASHSCOPE_API_KEY")
public class TestApiKey {
  static final String environmentValue = "1111";
  static final String environmentPath = "/tmp/ds_api_key";

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = environmentValue)
  public void testSetWithEnvValue() throws NoApiKeyException {
    Constants.apiKey = null; // Clear any previously set API key
    String apiKey = ApiKey.getApiKey(null);
    assertEquals(environmentValue, apiKey);
  }

  @Test
  public void testDirectSet() throws NoApiKeyException {
    String apiKeySet = "2222";
    String apiKey = ApiKey.getApiKey(apiKeySet);
    assertEquals(apiKey, apiKeySet);
  }

  @Test
  public void testSetWithConstants() throws NoApiKeyException {
    Constants.apiKey = "3333";
    String apiKey = ApiKey.getApiKey(null);
    assertEquals(Constants.apiKey, apiKey);
  }

  @Test
  public void testWithDefaultFile() throws NoApiKeyException, IOException {
    Constants.apiKey = null; // Clear any previously set API key
    Path homePath = Paths.get(System.getProperty("user.home"));
    Path dashscopePath = homePath.resolve(".dashscope").resolve("api_key");
    String expectedValue = "4444";
    // Delete if exists before creating
    if (Files.exists(dashscopePath.getParent()) && Files.isDirectory(dashscopePath.getParent())) {
      if (Files.exists(dashscopePath)) {
        Files.delete(dashscopePath);
      }
    }
    Files.createDirectories(dashscopePath.getParent());
    Files.write(
        dashscopePath,
        expectedValue.getBytes(),
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.CREATE);
    String apiKey = ApiKey.getApiKey(null);
    assertEquals(expectedValue, apiKey);
    Files.delete(dashscopePath);
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_API_KEY_FILE_PATH", value = environmentPath)
  @ClearEnvironmentVariable(key = "DASHSCOPE_API_KEY")
  public void testWithEnvFile() throws NoApiKeyException, IOException {
    Constants.apiKey = null; // Clear any previously set API key
    String expectedValue = "555";
    Files.write(
        Paths.get(environmentPath),
        expectedValue.getBytes(),
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.CREATE);
    String apiKey = ApiKey.getApiKey(null);
    assertEquals(expectedValue, apiKey);
  }
}
