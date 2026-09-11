// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.HalfDuplexTestParam;
import com.alibaba.dashscope.Version;
import com.alibaba.dashscope.audio.asr.phrase.AsrPhraseFinetuneOption;
import com.alibaba.dashscope.audio.asr.phrase.AsrPhraseOperationType;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.task.AsyncTaskParam;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/**
 * Locks the SDK tracking header contract. Exactly two tracking headers are emitted:
 *
 * <pre>
 *   x-dashscope-sdk-client:     java-sdk/&lt;version&gt;[/&lt;module&gt;]
 *   x-dashscope-sdk-session-id: &lt;process-wide uuid&gt;
 * </pre>
 *
 * No other {@code x-dashscope-sdk-*} header may be emitted.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class TestDashScopeHeaders {
  private static final String SDK_CLIENT = "x-dashscope-sdk-client";
  private static final String SDK_SESSION_ID = "x-dashscope-sdk-session-id";
  private static final String SDK_HEADER_PREFIX = "x-dashscope-sdk-";
  private static final String CLIENT_PREFIX = "java-sdk/" + Version.version;
  private static final String API_KEY = "sk-test";

  private static Map<String, String> httpHeaders(String module) throws NoApiKeyException {
    return DashScopeHeaders.buildHttpHeaders(
        API_KEY, false, Protocol.HTTP, false, false, null, new HashMap<>(), null, module);
  }

  private static Map<String, String> wsHeaders(String module) throws NoApiKeyException {
    return DashScopeHeaders.buildWebSocketHeaders(
        API_KEY, false, null, new HashMap<>(), null, module);
  }

  private static void assertOnlySdkTrackingHeaders(Map<String, String> headers) {
    for (String key : headers.keySet()) {
      if (key.startsWith(SDK_HEADER_PREFIX)) {
        assertTrue(SDK_CLIENT.equals(key) || SDK_SESSION_ID.equals(key), "unexpected: " + key);
      }
    }
  }

  @Test
  public void testSdkClientCarriesModuleSegment() throws NoApiKeyException {
    assertEquals(CLIENT_PREFIX + "/aigc", httpHeaders("aigc").get(SDK_CLIENT));
    assertEquals(CLIENT_PREFIX + "/audio", wsHeaders("audio").get(SDK_CLIENT));
  }

  @Test
  public void testSdkClientOmitsModuleWhenNullOrBlank() throws NoApiKeyException {
    for (String module : new String[] {null, ""}) {
      String value = httpHeaders(module).get(SDK_CLIENT);
      assertEquals(CLIENT_PREFIX, value);
      assertFalse(value.endsWith("/"));
    }
  }

  @Test
  public void testOnlyTwoTrackingHeadersAreEmitted() throws NoApiKeyException {
    for (Map<String, String> headers : new Map[] {httpHeaders("aigc"), wsHeaders("audio")}) {
      assertTrue(headers.get(SDK_CLIENT).startsWith(CLIENT_PREFIX));
      assertOnlySdkTrackingHeaders(headers);
    }
    assertOnlySdkTrackingHeaders(
        DashScopeHeaders.buildHttpHeaders(
            API_KEY, false, Protocol.HTTP, false, false, null, new HashMap<String, String>()));
  }

  @Test
  public void testLegacyOverloadsOmitModule() throws NoApiKeyException {
    assertEquals(
        CLIENT_PREFIX,
        DashScopeHeaders.buildHttpHeaders(
                API_KEY, false, Protocol.HTTP, false, false, null, new HashMap<String, String>())
            .get(SDK_CLIENT));
    // the 8th argument of this overload is customUserAgent, not module
    assertEquals(
        CLIENT_PREFIX,
        DashScopeHeaders.buildHttpHeaders(
                API_KEY,
                false,
                Protocol.HTTP,
                false,
                false,
                null,
                new HashMap<String, String>(),
                "my-app")
            .get(SDK_CLIENT));
    assertEquals(
        CLIENT_PREFIX,
        DashScopeHeaders.buildWebSocketHeaders(API_KEY, false, null, new HashMap<String, String>())
            .get(SDK_CLIENT));
  }

  @Test
  public void testSessionIdIsProcessWideUuid() throws NoApiKeyException {
    String fromHttp = httpHeaders("aigc").get(SDK_SESSION_ID);
    String fromWebSocket = wsHeaders("audio").get(SDK_SESSION_ID);
    assertEquals(fromHttp, fromWebSocket);
    assertEquals(fromHttp, UUID.fromString(fromHttp).toString());
  }

  @Test
  public void testCustomHeadersOverrideSdkClient() throws NoApiKeyException {
    Map<String, String> customHeaders = new HashMap<>();
    customHeaders.put(SDK_CLIENT, "my-client/9.9/my-module");
    assertEquals(
        "my-client/9.9/my-module",
        DashScopeHeaders.buildHttpHeaders(
                API_KEY, false, Protocol.HTTP, false, false, null, customHeaders, null, "aigc")
            .get(SDK_CLIENT));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS", value = "1")
  public void testDisableEnvVarSuppressesTrackingHeaders() throws NoApiKeyException {
    Map<String, String> headers = httpHeaders("aigc");
    assertNull(headers.get(SDK_CLIENT));
    assertNull(headers.get(SDK_SESSION_ID));
    // opting out must not drop auth or content negotiation
    assertTrue(headers.containsKey("Authorization"));
    assertTrue(headers.containsKey("user-agent"));
  }

  @Test
  @SetEnvironmentVariable(key = "DASHSCOPE_DISABLE_SDK_HEADERS", value = "TRUE")
  public void testDisableEnvVarIsCaseInsensitive() throws NoApiKeyException {
    assertNull(httpHeaders("aigc").get(SDK_CLIENT));
    assertNull(wsHeaders("audio").get(SDK_CLIENT));
  }

  @Test
  public void testApiServiceOptionPrefersTaskGroupThenFunction() {
    assertEquals(
        "aigc",
        ApiServiceOption.builder().taskGroup("aigc").function("generation").build().getModule());
    assertEquals(
        "tokenizer",
        ApiServiceOption.builder().taskGroup(null).function("tokenizer").build().getModule());
    assertNull(ApiServiceOption.builder().build().getModule());
  }

  @Test
  public void testAsyncTaskOptionReportsTasks() {
    assertEquals("tasks", AsyncTaskOption.builder().url("/tasks/task-id").build().getModule());
  }

  @Test
  public void testGeneralServiceOptionReportsConfiguredModule() {
    assertEquals(
        "agentstudio",
        GeneralServiceOption.builder().path("agents").module("agentstudio").build().getModule());
    assertNull(GeneralServiceOption.builder().path("agents").build().getModule());
  }

  @Test
  public void testAsrPhraseFinetuneOptionReportsAudio() {
    assertEquals(
        "audio",
        AsrPhraseFinetuneOption.builder()
            .operationType(AsrPhraseOperationType.CREATE)
            .build()
            .getModule());
  }

  @Test
  public void testRequestPropagatesModuleFromServiceOption()
      throws NoApiKeyException, ApiException {
    ApiServiceOption option =
        ApiServiceOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .taskGroup("aigc")
            .task("text-generation")
            .function("generation")
            .build();
    HalfDuplexTestParam param =
        HalfDuplexTestParam.builder().apiKey(API_KEY).model("qwen-turbo").prompt("hi").build();
    HalfDuplexRequest request = new HalfDuplexRequest(param, option);
    assertEquals("aigc", request.getModule());
    assertEquals(CLIENT_PREFIX + "/aigc", request.getHttpRequest().getHeaders().get(SDK_CLIENT));
  }

  @Test
  public void testAsyncTaskPollRequestCarriesTasksModule() throws NoApiKeyException, ApiException {
    AsyncTaskOption option =
        AsyncTaskOption.builder()
            .protocol(Protocol.HTTP)
            .httpMethod(HttpMethod.GET)
            .url("/tasks/task-id")
            .build();
    AsyncTaskParam param = AsyncTaskParam.builder().taskId("task-id").apiKey(API_KEY).build();
    HalfDuplexRequest request = new HalfDuplexRequest(param, option);
    assertEquals("tasks", request.getModule());
    assertEquals(CLIENT_PREFIX + "/tasks", request.getHttpRequest().getHeaders().get(SDK_CLIENT));
  }
}
