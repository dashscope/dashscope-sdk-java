// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.ClientOptions;
import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.v3.config.ConfigResolver;
import com.alibaba.dashscope.v3.config.ResolvedConfig;
import com.alibaba.dashscope.v3.errors.SdkException;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.v3.types.messages.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.ClearSystemProperty;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.junitpioneer.jupiter.SetSystemProperty;

/** Config chain: builder &gt; sysprop &gt; env &gt; config.toml &gt; default (design §2). */
@ClearEnvironmentVariable(key = "DASHSCOPE_API_KEY")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_WEBSOCKET_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@ClearEnvironmentVariable(key = "DASHSCOPE_TIMEOUT")
@ClearEnvironmentVariable(key = "DASHSCOPE_MAX_RETRIES")
@ClearEnvironmentVariable(key = "DASHSCOPE_WS_PING_INTERVAL")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "target/v3-test/config.toml")
@SetEnvironmentVariable(key = "DASHSCOPE_API_KEY_FILE_PATH", value = "target/v3-test/api_key")
@ClearSystemProperty(key = "dashscope.api.key")
@ClearSystemProperty(key = "dashscope.base.url")
@ClearSystemProperty(key = "dashscope.ws.base.url")
@ClearSystemProperty(key = "dashscope.workspace")
@ClearSystemProperty(key = "dashscope.region")
@ClearSystemProperty(key = "dashscope.timeout")
@ClearSystemProperty(key = "dashscope.max.retries")
@ClearSystemProperty(key = "dashscope.ws.ping.interval")
class ConfigChainTest {
  private static final Path CONFIG_PATH = Paths.get("target/v3-test/config.toml");
  private static final Path KEY_PATH = Paths.get("target/v3-test/api_key");

  @BeforeEach
  void cleanFiles() throws IOException {
    Files.createDirectories(CONFIG_PATH.getParent());
    Files.deleteIfExists(CONFIG_PATH);
    Files.deleteIfExists(KEY_PATH);
  }

  private static ResolvedConfig resolve() {
    return ConfigResolver.resolve(ClientOptions.builder().build());
  }

  @Test
  void defaultsResolve() {
    ResolvedConfig config = resolve();
    assertEquals("cn-beijing", config.getRegion());
    assertEquals("https://dashscope.aliyuncs.com/api/v1", config.getBaseUrl());
    assertEquals("wss://dashscope.aliyuncs.com/api-ws/v1/inference", config.getWsBaseUrl());
    assertEquals(Duration.ofSeconds(300), config.getTimeout());
    assertEquals(2, config.getMaxRetries());
    assertTrue(config.isRaiseOnError());
    assertEquals(Duration.ZERO, config.getWsPingInterval());
    assertNull(config.getApiKey());
    assertNull(config.getWorkspace());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WORKSPACE", value = "ws1")
  void workspaceGatewayDerived() {
    ResolvedConfig config = resolve();
    assertEquals("https://ws1.cn-beijing.maas.aliyuncs.com/api/v1", config.getBaseUrl());
    assertEquals(
        "wss://ws1.cn-beijing.maas.aliyuncs.com/api-ws/v1/inference", config.getWsBaseUrl());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WORKSPACE", value = "ws1")
  @SetEnvironmentVariable(key = "DASHSCOPE_REGION", value = "ap-southeast-1")
  void regionFromEnv() {
    ResolvedConfig config = resolve();
    assertEquals("ap-southeast-1", config.getRegion());
    assertEquals("https://ws1.ap-southeast-1.maas.aliyuncs.com/api/v1", config.getBaseUrl());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WORKSPACE", value = "ws-env")
  @SetSystemProperty(key = "dashscope.workspace", value = "ws-sys")
  void syspropBeatsEnv() {
    assertEquals("https://ws-sys.cn-beijing.maas.aliyuncs.com/api/v1", resolve().getBaseUrl());
  }

  @Test
  @SetSystemProperty(key = "dashscope.workspace", value = "ws-sys")
  void builderBeatsSysprop() {
    ResolvedConfig config =
        ConfigResolver.resolve(ClientOptions.builder().workspace("ws-builder").build());
    assertEquals("https://ws-builder.cn-beijing.maas.aliyuncs.com/api/v1", config.getBaseUrl());
  }

  @Test
  @SetEnvironmentVariable(
      key = "DASHSCOPE_HTTP_BASE_URL",
      value = "https://legacy.example.com/api/v1")
  void legacyHttpBaseUrlEnvHonored() {
    assertEquals("https://legacy.example.com/api/v1", resolve().getBaseUrl());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_BASE_URL", value = "https://new.example.com")
  @SetEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL", value = "https://legacy.example.com")
  void newBaseUrlEnvBeatsLegacy() {
    assertEquals("https://new.example.com", resolve().getBaseUrl());
  }

  @Test
  void configTomlTier() throws IOException {
    Files.write(
        CONFIG_PATH,
        Arrays.asList(
            "workspace = \"ws-file\"",
            "api_key = \"file-key\"",
            "timeout = 5",
            "max_retries = 4",
            "# a comment",
            "[section]"),
        StandardCharsets.UTF_8);

    ResolvedConfig config = resolve();
    assertEquals("ws-file", config.getWorkspace());
    assertEquals("file-key", config.getApiKey());
    assertEquals(Duration.ofSeconds(5), config.getTimeout());
    assertEquals(4, config.getMaxRetries());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WORKSPACE", value = "ws-env")
  void envBeatsConfigToml() throws IOException {
    Files.write(CONFIG_PATH, "workspace = \"ws-file\"".getBytes(StandardCharsets.UTF_8));
    assertEquals("ws-env", resolve().getWorkspace());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_TIMEOUT", value = "7.5")
  @SetEnvironmentVariable(key = "DASHSCOPE_MAX_RETRIES", value = "5")
  void timeoutAndRetriesFromEnv() {
    ResolvedConfig config = resolve();
    assertEquals(Duration.ofMillis(7500), config.getTimeout());
    assertEquals(5, config.getMaxRetries());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "env-key")
  @SetSystemProperty(key = "dashscope.api.key", value = "sys-key")
  void apiKeySyspropBeatsEnv() {
    assertEquals("sys-key", resolve().getApiKey());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "env-key")
  void apiKeyFromEnv() {
    assertEquals("env-key", resolve().getApiKey());
  }

  @Test
  void apiKeyFromKeyFile() throws IOException {
    Files.write(KEY_PATH, "file-api-key\n".getBytes(StandardCharsets.UTF_8));
    assertEquals("file-api-key", resolve().getApiKey());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_API_KEY", value = "env-key")
  void apiKeyEnvBeatsKeyFile() throws IOException {
    Files.write(KEY_PATH, "file-api-key".getBytes(StandardCharsets.UTF_8));
    assertEquals("env-key", resolve().getApiKey());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WS_PING_INTERVAL", value = "25")
  void wsPingIntervalFromEnv() {
    assertEquals(Duration.ofSeconds(25), resolve().getWsPingInterval());
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_WS_PING_INTERVAL", value = "25")
  @SetSystemProperty(key = "dashscope.ws.ping.interval", value = "10.5")
  void wsPingIntervalSyspropBeatsEnv() {
    assertEquals(Duration.ofMillis(10500), resolve().getWsPingInterval());
  }

  @Test
  @SetSystemProperty(key = "dashscope.ws.ping.interval", value = "10")
  void wsPingIntervalBuilderBeatsSysprop() {
    ResolvedConfig config =
        ConfigResolver.resolve(
            ClientOptions.builder().wsPingInterval(Duration.ofSeconds(3)).build());
    assertEquals(Duration.ofSeconds(3), config.getWsPingInterval());
  }

  @Test
  void wsPingIntervalFromConfigToml() throws IOException {
    Files.write(CONFIG_PATH, "ws_ping_interval = 15".getBytes(StandardCharsets.UTF_8));
    assertEquals(Duration.ofSeconds(15), resolve().getWsPingInterval());
  }

  @Test
  void invalidRegionRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ConfigResolver.resolve(ClientOptions.builder().region("cn-nowhere").build()));
  }

  @Test
  void missingApiKeyThrowsSdkExceptionBeforeAnyHttp() {
    try (DashScopeClient client =
        DashScopeClient.builder().baseUrl("http://127.0.0.1:1").maxRetries(0).build()) {
      SdkException e =
          assertThrows(
              SdkException.class,
              () ->
                  client
                      .chat()
                      .completions()
                      .create(
                          ChatCompletionCreateParams.builder()
                              .model("qwen-plus")
                              .messages(Collections.singletonList(Message.user("hi")))
                              .build()));
      assertEquals("sdk.InvalidApiKey", e.getCode());
      assertFalse(e.isAllowRetry());
      assertTrue(e.getSolution().contains("DASHSCOPE_API_KEY"));
    }
  }

  @Test
  void deriveHonorsNewlyInjectedHttpClient() {
    ResolvedConfig base = ConfigResolver.resolve(ClientOptions.builder().build());
    try (HttpTransport transport = new HttpTransport(base)) {
      OkHttpClient injected = new OkHttpClient();
      try {
        ResolvedConfig withInjected =
            ConfigResolver.resolve(ClientOptions.builder().httpClient(injected).build());
        try (HttpTransport derived = transport.derive(withInjected)) {
          assertSame(injected, derived.okhttp());
        }
        ResolvedConfig withoutInjected =
            ConfigResolver.resolve(ClientOptions.builder().timeout(Duration.ofSeconds(9)).build());
        try (HttpTransport derived = transport.derive(withoutInjected)) {
          assertSame(transport.okhttp(), derived.okhttp());
        }
      } finally {
        injected.connectionPool().evictAll();
        injected.dispatcher().executorService().shutdown();
      }
    }
  }

  @Test
  void withOptionsHonorsInjectedHttpClient() {
    OkHttpClient injected = new OkHttpClient();
    try (DashScopeClient client =
        DashScopeClient.builder().apiKey("k").baseUrl("http://127.0.0.1:1").maxRetries(0).build()) {
      try (DashScopeClient derived =
          client.withOptions(ClientOptions.builder().httpClient(injected).build())) {
        assertSame(injected, derived.config().getHttpClient());
      }
      try (DashScopeClient derived =
          client.withOptions(ClientOptions.builder().timeout(Duration.ofSeconds(9)).build())) {
        assertNull(derived.config().getHttpClient());
      }
    } finally {
      injected.connectionPool().evictAll();
      injected.dispatcher().executorService().shutdown();
    }
  }
}
