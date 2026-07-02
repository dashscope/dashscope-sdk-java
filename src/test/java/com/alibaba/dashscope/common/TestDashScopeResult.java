// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.base.HalfDuplexParamBase;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.protocol.HalfDuplexRequest;
import com.alibaba.dashscope.protocol.HttpMethod;
import com.alibaba.dashscope.protocol.NetworkResponse;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.protocol.ServiceOption;
import com.alibaba.dashscope.protocol.StreamingMode;
import com.alibaba.dashscope.utils.EncryptionConfig;
import com.alibaba.dashscope.utils.EncryptionUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DashScopeResult} focusing on output field parsing type-safety, flatten
 * mode behavior, and encryption fallback decryption.
 */
public class TestDashScopeResult {

  private NetworkResponse buildHttpResponse(String body) {
    return NetworkResponse.builder()
        .message(body)
        .headers(new HashMap<>())
        .httpStatusCode(200)
        .build();
  }

  private NetworkResponse buildHttpResponse(String body, Map<String, List<String>> headers) {
    return NetworkResponse.builder()
        .message(body)
        .headers(headers)
        .httpStatusCode(200)
        .build();
  }

  @Test
  public void testOutputAsJsonObject() throws ApiException {
    String json = "{\"output\":{\"text\":\"hello\"},\"request_id\":\"req-1\"}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    JsonObject output = (JsonObject) result.getOutput();
    assertEquals("hello", output.get("text").getAsString());
    assertEquals("req-1", result.getRequestId());
  }

  @Test
  public void testOutputAsJsonPrimitiveNoThrow() throws ApiException {
    String json = "{\"output\":\"some-plain-string\",\"request_id\":\"req-2\"}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonElement);
    assertEquals("some-plain-string", ((JsonElement) result.getOutput()).getAsString());
  }

  @Test
  public void testOutputAsJsonNull() throws ApiException {
    String json = "{\"output\":null,\"request_id\":\"req-3\"}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp);

    assertNull(result.getOutput());
  }

  @Test
  public void testOutputFieldAbsent() throws ApiException {
    String json = "{\"request_id\":\"req-4\",\"code\":\"0\",\"message\":\"ok\"}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp);

    assertNull(result.getOutput());
    assertEquals("req-4", result.getRequestId());
    assertEquals("0", result.getCode());
  }

  @Test
  public void testIsFlattenHttpReturnsEntireJson() throws ApiException {
    String json =
        "{\"output\":{\"text\":\"hello\"},\"request_id\":\"req-5\",\"usage\":{\"total\":10}}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp, true);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    JsonObject output = (JsonObject) result.getOutput();
    assertTrue(output.has("output"));
    assertTrue(output.has("request_id"));
    assertTrue(output.has("usage"));
    assertEquals("hello", output.getAsJsonObject("output").get("text").getAsString());
  }

  @Test
  public void testIsFlattenWebSocketReturnsEntireJson() throws ApiException {
    String json =
        "{\"header\":{\"task_id\":\"task-1\"},\"payload\":{\"output\":{\"text\":\"hi\"}}}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.WEBSOCKET, resp, true);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    JsonObject output = (JsonObject) result.getOutput();
    assertTrue(output.has("header"));
    assertTrue(output.has("payload"));
  }

  @Test
  public void testWebSocketNonFlattenOutput() throws ApiException {
    String json =
        "{\"header\":{\"task_id\":\"task-2\",\"status_code\":200},"
            + "\"payload\":{\"output\":{\"text\":\"ws-hello\"}}}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.WEBSOCKET, resp);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    assertEquals("ws-hello", ((JsonObject) result.getOutput()).get("text").getAsString());
    assertEquals("task-2", result.getRequestId());
    assertEquals(Integer.valueOf(200), result.getStatusCode());
  }

  @Test
  public void testOutputWithDataField() throws ApiException {
    String json = "{\"data\":{\"key\":\"val\"},\"request_id\":\"req-8\"}";
    NetworkResponse resp = buildHttpResponse(json);
    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    JsonObject output = (JsonObject) result.getOutput();
    assertTrue(output.has("data"));
    assertFalse(output.has("request_id"));
  }

  @Test
  public void testEncryptionFallbackDecryption() throws Exception {
    SecretKey aesKey = EncryptionUtils.generateAESKey();
    byte[] iv = new byte[12];
    new java.security.SecureRandom().nextBytes(iv);

    String plainOutput = "{\"text\":\"decrypted-content\"}";
    String encryptedOutput = EncryptionUtils.AESEncrypt(plainOutput, aesKey, iv);

    String json = "{\"output\":\"" + encryptedOutput + "\",\"request_id\":\"req-9\"}";
    NetworkResponse resp = buildHttpResponse(json);

    HalfDuplexRequest req = buildTestHalfDuplexRequest(false, aesKey, iv);

    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp, false, req);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    assertEquals("decrypted-content", ((JsonObject) result.getOutput()).get("text").getAsString());
    assertEquals("req-9", result.getRequestId());
  }

  @Test
  public void testNoFallbackWhenConfigNull() throws Exception {
    String base64LikeString =
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String json = "{\"output\":\"" + base64LikeString + "\",\"request_id\":\"req-10\"}";
    NetworkResponse resp = buildHttpResponse(json);

    HalfDuplexRequest req = buildTestHalfDuplexRequest(false, null, null);

    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp, false, req);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonElement);
    assertEquals(base64LikeString, ((JsonElement) result.getOutput()).getAsString());
  }

  @Test
  public void testEncryptionWithHeader() throws Exception {
    SecretKey aesKey = EncryptionUtils.generateAESKey();
    byte[] iv = new byte[12];
    new java.security.SecureRandom().nextBytes(iv);

    String plainOutput = "{\"text\":\"header-decrypted\"}";
    String encryptedOutput = EncryptionUtils.AESEncrypt(plainOutput, aesKey, iv);

    String json = "{\"output\":\"" + encryptedOutput + "\",\"request_id\":\"req-11\"}";

    Map<String, List<String>> headers = new HashMap<>();
    headers.put("x-dashscope-outputencrypted", Arrays.asList("true"));
    NetworkResponse resp = buildHttpResponse(json, headers);

    HalfDuplexRequest req = buildTestHalfDuplexRequest(true, aesKey, iv);

    DashScopeResult result = new DashScopeResult();
    result.fromResponse(Protocol.HTTP, resp, false, req);

    assertNotNull(result.getOutput());
    assertTrue(result.getOutput() instanceof JsonObject);
    assertEquals("header-decrypted", ((JsonObject) result.getOutput()).get("text").getAsString());
  }

  // ---- Helpers ----

  @SuperBuilder
  private static class TestParamBase extends HalfDuplexParamBase {
    @Override
    public String getModel() {
      return "test-model";
    }

    @Override
    public Map<String, Object> getParameters() {
      return new HashMap<>();
    }

    @Override
    public Map<String, String> getHeaders() {
      return new HashMap<>();
    }

    @Override
    public JsonObject getHttpBody() {
      return new JsonObject();
    }

    @Override
    public Object getInput() {
      return null;
    }

    @Override
    public Object getResources() {
      return null;
    }

    @Override
    public ByteBuffer getBinaryData() {
      return null;
    }

    @Override
    public void validate() {}
  }

  private static class TestServiceOption implements ServiceOption {
    @Override
    public StreamingMode getStreamingMode() {
      return null;
    }

    @Override
    public Protocol getProtocol() {
      return Protocol.HTTP;
    }

    @Override
    public HttpMethod getHttpMethod() {
      return HttpMethod.POST;
    }

    @Override
    public String httpUrl() {
      return "/test";
    }

    @Override
    public String getBaseHttpUrl() {
      return null;
    }

    @Override
    public String getBaseWebSocketUrl() {
      return null;
    }
  }

  private HalfDuplexRequest buildTestHalfDuplexRequest(
      boolean enableEncrypt, SecretKey aesKey, byte[] iv) throws Exception {
    TestParamBase param = TestParamBase.builder().enableEncrypt(enableEncrypt).build();

    HalfDuplexRequest req = new HalfDuplexRequest(param, new TestServiceOption());

    if (aesKey != null) {
      EncryptionConfig config =
          EncryptionConfig.builder()
              .publicKeyId("test-key-id")
              .base64PublicKey("test-public-key")
              .AESEncryptKey(aesKey)
              .iv(iv)
              .build();
      Field f = HalfDuplexRequest.class.getDeclaredField("encryptionConfig");
      f.setAccessible(true);
      f.set(req, config);
    }
    return req;
  }
}
