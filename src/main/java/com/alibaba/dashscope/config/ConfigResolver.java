// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.config;

import com.alibaba.dashscope.ClientOptions;
import com.alibaba.dashscope.shared.EnvNames;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration resolution (design §2). Priority per key: explicit builder arg &gt; JVM system
 * property &gt; environment variable &gt; {@code ~/.dashscope/config.toml} &gt; built-in default.
 * No mutable global facade — 2.x {@code Constants} global state is a documented historical burden
 * and 3.0 does not repeat it.
 */
public final class ConfigResolver {
  private ConfigResolver() {}

  private static final Logger log = LoggerFactory.getLogger(ConfigResolver.class);
  private static final AtomicBoolean LEGACY_HTTP_WARNED = new AtomicBoolean(false);
  private static final AtomicBoolean LEGACY_WS_WARNED = new AtomicBoolean(false);

  static final String SYSPROP_BASE_URL = "dashscope.base.url";
  static final String SYSPROP_WS_BASE_URL = "dashscope.ws.base.url";
  static final String SYSPROP_WORKSPACE = "dashscope.workspace";
  static final String SYSPROP_REGION = "dashscope.region";
  static final String SYSPROP_TIMEOUT = "dashscope.timeout";
  static final String SYSPROP_MAX_RETRIES = "dashscope.max.retries";

  public static final String DEFAULT_REGION = "cn-beijing";
  public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(300);
  public static final int DEFAULT_MAX_RETRIES = 2;
  public static final int DEFAULT_MAX_REQUESTS = 100;
  public static final int DEFAULT_MAX_REQUESTS_PER_HOST = 20;
  public static final Duration DEFAULT_KEEP_ALIVE = Duration.ofSeconds(5);

  private static final String NATIVE_API_PATH = "/api/v1";
  private static final String WS_INFERENCE_PATH = "/api-ws/v1/inference";
  private static final String WORKSPACE_GATEWAY_TEMPLATE = "https://%s.%s.maas.aliyuncs.com";
  private static final String WORKSPACE_WS_GATEWAY_TEMPLATE = "wss://%s.%s.maas.aliyuncs.com";

  /** The MaaS gateway is deployed in six regions. */
  private static final String[] VALID_REGIONS = {
    "cn-beijing", "ap-southeast-1", "cn-hongkong", "us-east-1", "eu-central-1", "ap-northeast-1"
  };

  /** Legacy shared gateways (2.x endpoints), per region; used only as the no-workspace
   * fallback so an apiKey-only quickstart keeps working. Regions without a dedicated shared
   * gateway use the international one. */
  private static final Map<String, String> LEGACY_GATEWAY_HOSTS;
  private static final Map<String, String> LEGACY_WS_GATEWAY_HOSTS;

  static {
    Map<String, String> http = new HashMap<>();
    http.put("cn-beijing", "https://dashscope.aliyuncs.com");
    http.put("ap-southeast-1", "https://dashscope-intl.aliyuncs.com");
    http.put("cn-hongkong", "https://cn-hongkong.dashscope.aliyuncs.com");
    http.put("us-east-1", "https://dashscope-intl.aliyuncs.com");
    http.put("eu-central-1", "https://dashscope-intl.aliyuncs.com");
    http.put("ap-northeast-1", "https://dashscope-intl.aliyuncs.com");
    LEGACY_GATEWAY_HOSTS = Collections.unmodifiableMap(http);

    Map<String, String> ws = new HashMap<>();
    ws.put("cn-beijing", "wss://dashscope.aliyuncs.com");
    ws.put("ap-southeast-1", "wss://dashscope-intl.aliyuncs.com");
    ws.put("cn-hongkong", "wss://cn-hongkong.dashscope.aliyuncs.com");
    ws.put("us-east-1", "wss://dashscope-intl.aliyuncs.com");
    ws.put("eu-central-1", "wss://dashscope-intl.aliyuncs.com");
    ws.put("ap-northeast-1", "wss://dashscope-intl.aliyuncs.com");
    LEGACY_WS_GATEWAY_HOSTS = Collections.unmodifiableMap(ws);
  }

  public static boolean isValidRegion(String region) {
    for (String r : VALID_REGIONS) {
      if (r.equals(region)) {
        return true;
      }
    }
    return false;
  }

  public static ResolvedConfig resolve(ClientOptions options) {
    Map<String, String> file = ConfigFile.load(ConfigFile.defaultPath());

    String region = firstNonBlank(
        options.getRegion(),
        System.getProperty(SYSPROP_REGION),
        System.getenv(EnvNames.DASHSCOPE_REGION),
        file.get(ConfigFile.KEY_REGION));
    region = region == null ? DEFAULT_REGION : region;
    if (!isValidRegion(region)) {
      throw new IllegalArgumentException(
          "invalid region '" + region + "', expected one of " + String.join(", ", VALID_REGIONS));
    }

    String workspace = firstNonBlank(
        options.getWorkspace(),
        System.getProperty(SYSPROP_WORKSPACE),
        System.getenv(EnvNames.DASHSCOPE_WORKSPACE),
        file.get(ConfigFile.KEY_WORKSPACE));

    String baseUrl = firstNonBlank(
        options.getBaseUrl(),
        System.getProperty(SYSPROP_BASE_URL),
        System.getenv(EnvNames.DASHSCOPE_BASE_URL),
        // 2.x env name, honored as fallback so existing setups keep working
        System.getenv(EnvNames.DASHSCOPE_HTTP_BASE_URL),
        file.get(ConfigFile.KEY_BASE_URL));
    if (baseUrl == null) {
      if (workspace != null) {
        baseUrl = String.format(WORKSPACE_GATEWAY_TEMPLATE, workspace, region) + NATIVE_API_PATH;
      } else {
        baseUrl = LEGACY_GATEWAY_HOSTS.get(region) + NATIVE_API_PATH;
        warnLegacyOnce(LEGACY_HTTP_WARNED, baseUrl);
      }
    }

    String wsBaseUrl = firstNonBlank(
        options.getWsBaseUrl(),
        System.getProperty(SYSPROP_WS_BASE_URL),
        System.getenv(EnvNames.DASHSCOPE_WEBSOCKET_BASE_URL));
    if (wsBaseUrl == null) {
      if (workspace != null) {
        wsBaseUrl =
            String.format(WORKSPACE_WS_GATEWAY_TEMPLATE, workspace, region) + WS_INFERENCE_PATH;
      } else {
        wsBaseUrl = LEGACY_WS_GATEWAY_HOSTS.get(region) + WS_INFERENCE_PATH;
        warnLegacyOnce(LEGACY_WS_WARNED, wsBaseUrl);
      }
    }

    Duration timeout = firstDuration(
        options.getTimeout(),
        System.getProperty(SYSPROP_TIMEOUT),
        System.getenv(EnvNames.DASHSCOPE_TIMEOUT),
        file.get(ConfigFile.KEY_TIMEOUT),
        DEFAULT_TIMEOUT);

    int maxRetries = firstInt(
        options.getMaxRetries(),
        System.getProperty(SYSPROP_MAX_RETRIES),
        System.getenv(EnvNames.DASHSCOPE_MAX_RETRIES),
        file.get(ConfigFile.KEY_MAX_RETRIES),
        DEFAULT_MAX_RETRIES);

    int maxRequests = envInt(EnvNames.DASHSCOPE_HTTP_MAX_CONNECTIONS, DEFAULT_MAX_REQUESTS);
    int maxRequestsPerHost =
        envInt(EnvNames.DASHSCOPE_HTTP_MAX_KEEPALIVE_CONNECTIONS, DEFAULT_MAX_REQUESTS_PER_HOST);
    long keepAliveSeconds =
        envLong(EnvNames.DASHSCOPE_HTTP_KEEPALIVE_EXPIRY, DEFAULT_KEEP_ALIVE.getSeconds());

    String apiKey = ApiKeyResolver.resolve(options.getApiKey(), file);

    return new ResolvedConfig(
        apiKey,
        stripTrailingSlash(baseUrl),
        stripTrailingSlash(wsBaseUrl),
        workspace,
        region,
        timeout,
        maxRetries,
        options.getRaiseOnError() == null || options.getRaiseOnError(),
        options.getHttpClient(),
        maxRequests,
        maxRequestsPerHost,
        Duration.ofSeconds(keepAliveSeconds));
  }

  private static void warnLegacyOnce(AtomicBoolean flag, String url) {
    if (flag.compareAndSet(false, true)) {
      log.warn(
          "no workspace configured: falling back to the legacy shared gateway ({}). "
              + "For better performance and stability, configure a workspace via "
              + "DASHSCOPE_WORKSPACE, builder.workspace(...), or config.toml.",
          url);
    }
  }

  private static String stripTrailingSlash(String url) {
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  private static String firstNonBlank(String... values) {
    return ApiKeyResolver.firstNonBlank(values);
  }

  private static Duration firstDuration(
      Duration explicit, String sysprop, String env, String fileValue, Duration fallback) {
    if (explicit != null) {
      return explicit;
    }
    for (String raw : new String[] {sysprop, env, fileValue}) {
      if (raw != null && !raw.trim().isEmpty()) {
        try {
          return Duration.ofMillis((long) (Double.parseDouble(raw.trim()) * 1000));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("invalid timeout value: '" + raw + "'", e);
        }
      }
    }
    return fallback;
  }

  private static int firstInt(
      Integer explicit, String sysprop, String env, String fileValue, int fallback) {
    if (explicit != null) {
      return explicit;
    }
    for (String raw : new String[] {sysprop, env, fileValue}) {
      if (raw != null && !raw.trim().isEmpty()) {
        try {
          return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("invalid integer value: '" + raw + "'", e);
        }
      }
    }
    return fallback;
  }

  private static int envInt(String name, int fallback) {
    String raw = System.getenv(name);
    if (raw == null || raw.trim().isEmpty()) {
      return fallback;
    }
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static long envLong(String name, long fallback) {
    String raw = System.getenv(name);
    if (raw == null || raw.trim().isEmpty()) {
      return fallback;
    }
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
