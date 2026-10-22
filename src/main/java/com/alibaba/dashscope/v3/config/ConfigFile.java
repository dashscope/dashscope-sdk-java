// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.config;

import com.alibaba.dashscope.shared.EnvNames;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Minimal reader for {@code ~/.dashscope/config.toml}. Flat subset only: top-level {@code key =
 * value} lines with string / integer / float / boolean values; sections and comments are skipped.
 * No TOML dependency is pulled in (design §1).
 */
public final class ConfigFile {
  private ConfigFile() {}

  public static final String DEFAULT_PATH = "~/.dashscope/config.toml";

  /** Keys honored from the file (mirrors the Python 2.0 set, minus protocol which 3.0 M1
   * does not implement yet). */
  static final String KEY_API_KEY = "api_key";
  static final String KEY_BASE_URL = "base_url";
  static final String KEY_WORKSPACE = "workspace";
  static final String KEY_REGION = "region";
  static final String KEY_TIMEOUT = "timeout";
  static final String KEY_MAX_RETRIES = "max_retries";

  /** Parse the file at {@code path}; missing file yields an empty map. Malformed lines are
   * skipped (config discovery must never crash client construction). */
  public static Map<String, String> load(Path path) {
    if (path == null || !Files.isRegularFile(path)) {
      return Collections.emptyMap();
    }
    Map<String, String> out = new HashMap<>();
    try {
      for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) {
          continue;
        }
        int eq = trimmed.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        String key = trimmed.substring(0, eq).trim();
        String value = trimmed.substring(eq + 1).trim();
        int comment = value.indexOf('#');
        if (comment > 0 && !value.startsWith("\"")) {
          value = value.substring(0, comment).trim();
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
          value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (!key.isEmpty() && !value.isEmpty()) {
          out.put(key, value);
        }
      }
    } catch (IOException e) {
      return Collections.emptyMap();
    }
    return out;
  }

  /** Config file path: {@code DASHSCOPE_CONFIG} env, then the default. */
  public static Path defaultPath() {
    String fromEnv = System.getenv(EnvNames.DASHSCOPE_CONFIG);
    String raw = (fromEnv == null || fromEnv.isEmpty()) ? DEFAULT_PATH : fromEnv;
    if (raw.startsWith("~/")) {
      raw = System.getProperty("user.home") + raw.substring(1);
    }
    return Paths.get(raw);
  }
}
