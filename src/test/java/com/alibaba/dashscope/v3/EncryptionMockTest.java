// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.ClientOptions;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.v3.config.ConfigResolver;
import com.alibaba.dashscope.v3.errors.ApiStatusException;
import com.alibaba.dashscope.v3.transport.EncryptionConfig;
import com.alibaba.dashscope.v3.transport.EncryptionKeys;
import com.alibaba.dashscope.v3.transport.EncryptionUtils;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ClearEnvironmentVariable;
import org.junitpioneer.jupiter.SetEnvironmentVariable;

/** J7 encryption channel: crypto roundtrips + public-key fetch wire shape. */
@ClearEnvironmentVariable(key = "DASHSCOPE_WORKSPACE")
@ClearEnvironmentVariable(key = "DASHSCOPE_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_HTTP_BASE_URL")
@ClearEnvironmentVariable(key = "DASHSCOPE_REGION")
@SetEnvironmentVariable(key = "DASHSCOPE_CONFIG", value = "/nonexistent/dashscope-config.toml")
class EncryptionMockTest {
  private MockWebServer server;
  private HttpTransport transport;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
    transport =
        new HttpTransport(
            ConfigResolver.resolve(
                ClientOptions.builder()
                    .apiKey("test-key")
                    .baseUrl(server.url("/").toString())
                    .build()));
  }

  @AfterEach
  void tearDown() throws Exception {
    transport.close();
    server.shutdown();
  }

  @Test
  void aesRoundTripRestoresPlainText() {
    SecretKey key = EncryptionUtils.generateAesKey();
    byte[] iv = EncryptionUtils.generateIv();
    String cipher = EncryptionUtils.aesEncrypt("敏感数据-42", key, iv);
    assertNotEquals("敏感数据-42", cipher);
    assertEquals("敏感数据-42", EncryptionUtils.aesDecrypt(cipher, key, iv));
  }

  @Test
  void aesDecryptWithWrongIvRaises() {
    SecretKey key = EncryptionUtils.generateAesKey();
    String cipher = EncryptionUtils.aesEncrypt("data", key, EncryptionUtils.generateIv());
    assertThrows(
        IllegalArgumentException.class,
        () -> EncryptionUtils.aesDecrypt(cipher, key, EncryptionUtils.generateIv()));
  }

  @Test
  void rsaEncryptIsDecryptableWithPrivateKey() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    java.security.KeyPair pair = gen.generateKeyPair();
    String base64Public = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

    String cipher = EncryptionUtils.rsaEncrypt("wrapped-aes-key", base64Public);

    Cipher rsa = Cipher.getInstance("RSA");
    rsa.init(Cipher.DECRYPT_MODE, pair.getPrivate());
    String plain = new String(rsa.doFinal(Base64.getDecoder().decode(cipher)), StandardCharsets.UTF_8);
    assertEquals("wrapped-aes-key", plain);
  }

  @Test
  void rsaEncryptRejectsGarbageKey() {
    assertThrows(
        IllegalArgumentException.class,
        () -> EncryptionUtils.rsaEncrypt("x", Base64.getEncoder().encodeToString(new byte[] {1, 2})));
  }

  @Test
  void createConfigFetchesLatestKeyAndBuildsHeader() throws Exception {
    KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
    gen.initialize(2048);
    String base64Public =
        Base64.getEncoder().encodeToString(gen.generateKeyPair().getPublic().getEncoded());
    server.enqueue(
        new MockResponse()
            .setBody(
                "{\"request_id\":\"req-1\",\"data\":{\"public_key\":\""
                    + base64Public
                    + "\",\"public_key_id\":\"key-42\"}}"));

    EncryptionConfig config =
        new EncryptionKeys(transport).createConfig(RequestOptions.NONE);

    assertEquals("key-42", config.getPublicKeyId());
    assertEquals(base64Public, config.getBase64PublicKey());
    assertEquals(12, config.getIv().length);
    String[] parts = config.keyHeader().split(",");
    assertEquals(3, parts.length);
    assertEquals("key-42", parts[0]);

    RecordedRequest request = server.takeRequest();
    assertEquals("/public-keys/latest", request.getPath());
    assertEquals("GET", request.getMethod());
    assertEquals("Bearer test-key", request.getHeader("Authorization"));
  }

  @Test
  void malformedKeyResponseRaisesResponseError() {
    server.enqueue(new MockResponse().setBody("{\"request_id\":\"req-2\"}"));
    ApiStatusException e =
        assertThrows(
            ApiStatusException.class,
            () -> new EncryptionKeys(transport).createConfig(RequestOptions.NONE));
    assertEquals(PublicErrorCode.RESPONSE_ERROR.getErrorCode(), e.getCode());
  }
}
