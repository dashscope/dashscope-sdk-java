// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.dashscope.config.ConfigResolver;
import com.alibaba.dashscope.errors.ApiStatusException;
import com.alibaba.dashscope.errors.PublicErrorCode;
import com.alibaba.dashscope.transport.EncryptionConfig;
import com.alibaba.dashscope.transport.EncryptionKeys;
import com.alibaba.dashscope.transport.EncryptionUtils;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.completions.ChatCompletion;
import com.alibaba.dashscope.types.completions.ChatCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.mockwebserver.Dispatcher;
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
  private static final ObjectMapper MAPPER = new ObjectMapper();

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
    String plain =
        new String(rsa.doFinal(Base64.getDecoder().decode(cipher)), StandardCharsets.UTF_8);
    assertEquals("wrapped-aes-key", plain);
  }

  @Test
  void rsaEncryptRejectsGarbageKey() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            EncryptionUtils.rsaEncrypt("x", Base64.getEncoder().encodeToString(new byte[] {1, 2})));
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

    EncryptionConfig config = new EncryptionKeys(transport).createConfig(RequestOptions.NONE);

    assertEquals("key-42", config.getPublicKeyId());
    assertEquals(base64Public, config.getBase64PublicKey());
    assertEquals(12, config.getIv().length);
    JsonNode header = MAPPER.readTree(config.keyHeader());
    assertEquals("key-42", header.get("public_key_id").asText());
    assertNotNull(header.get("encrypt_key").asText());
    assertNotNull(header.get("iv").asText());

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

  @Test
  void encryptedChatRoundTripUsesClientRequestPipeline() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();
    String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    AtomicReference<String> decryptedInput = new AtomicReference<>();
    server.setDispatcher(
        new Dispatcher() {
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            try {
              if ("/public-keys/latest".equals(request.getPath())) {
                return new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(
                        "{\"request_id\":\"key-request\",\"data\":{\"public_key\":\""
                            + publicKey
                            + "\",\"public_key_id\":\"key-99\"}}");
              }
              JsonNode keyHeader = MAPPER.readTree(request.getHeader("X-DashScope-EncryptionKey"));
              Cipher rsa = Cipher.getInstance("RSA");
              rsa.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
              byte[] aesBytes =
                  Base64.getDecoder()
                      .decode(
                          new String(
                              rsa.doFinal(
                                  Base64.getDecoder()
                                      .decode(keyHeader.get("encrypt_key").asText())),
                              StandardCharsets.UTF_8));
              SecretKey aesKey = new SecretKeySpec(aesBytes, "AES");
              byte[] iv = Base64.getDecoder().decode(keyHeader.get("iv").asText());
              JsonNode requestBody = MAPPER.readTree(request.getBody().readUtf8());
              decryptedInput.set(
                  EncryptionUtils.aesDecrypt(requestBody.get("input").asText(), aesKey, iv));

              String output =
                  "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                      + "\"content\":\"encrypted-ok\"}}]}";
              String encryptedOutput = EncryptionUtils.aesEncrypt(output, aesKey, iv);
              return new MockResponse()
                  .setHeader("Content-Type", "application/json")
                  .setHeader("X-DashScope-OutputEncrypted", "true")
                  .setBody(
                      "{\"request_id\":\"encrypted-1\",\"output\":\"" + encryptedOutput + "\"}");
            } catch (Exception e) {
              return new MockResponse().setResponseCode(500).setBody(e.toString());
            }
          }
        });

    try (DashScopeClient client =
        DashScopeClient.builder()
            .apiKey("test-key")
            .baseUrl(server.url("/").toString())
            .maxRetries(0)
            .build()) {
      ChatCompletion result =
          client
              .chat()
              .completions()
              .create(
                  ChatCompletionCreateParams.builder()
                      .model("qwen-plus")
                      .messages(Arrays.asList(Message.user("secret")))
                      .build(),
                  RequestOptions.builder().enableEncryption(true).build());

      assertEquals("encrypted-ok", result.outputText());
      JsonNode plainInput = MAPPER.readTree(decryptedInput.get());
      assertEquals("secret", plainInput.get("messages").get(0).get("content").asText());
    }
  }
}
