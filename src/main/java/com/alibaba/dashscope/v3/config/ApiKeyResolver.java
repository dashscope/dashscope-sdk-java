// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.config;

import com.alibaba.dashscope.shared.EnvNames;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * API key resolution chain (design J3). Priority: explicit builder arg &gt; JVM system property
 * {@code dashscope.api.key} &gt; {@code DASHSCOPE_API_KEY} env &gt; {@code config.toml} api_key
 * &gt; 1.x key file ({@code DASHSCOPE_API_KEY_FILE_PATH} or {@code ~/.dashscope/api_key}), so 1.x
 * users who stored their key via save_api_key keep working.
 */
public final class ApiKeyResolver {
  private ApiKeyResolver() {}

  static final String SYSPROP_API_KEY = "dashscope.api.key";
  private static final String DEFAULT_KEY_FILE = "~/.dashscope/api_key";

  public static String resolve(String explicit, Map<String, String> configFile) {
    String key =
        firstNonBlank(
            explicit,
            System.getProperty(SYSPROP_API_KEY),
            System.getenv(EnvNames.DASHSCOPE_API_KEY),
            configFile.get(ConfigFile.KEY_API_KEY));
    if (key != null) {
      return key;
    }
    return readKeyFile();
  }

  private static String readKeyFile() {
    String raw = System.getenv(EnvNames.DASHSCOPE_API_KEY_FILE_PATH);
    if (raw == null || raw.isEmpty()) {
      raw = DEFAULT_KEY_FILE;
    }
    if (raw.startsWith("~/")) {
      raw = System.getProperty("user.home") + raw.substring(1);
    }
    Path path = Paths.get(raw);
    if (!Files.isRegularFile(path)) {
      return null;
    }
    try {
      String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
      return content.isEmpty() ? null : content;
    } catch (IOException e) {
      return null;
    }
  }

  static String firstNonBlank(String... values) {
    for (String v : values) {
      if (v != null && !v.trim().isEmpty()) {
        return v.trim();
      }
    }
    return null;
  }
}
