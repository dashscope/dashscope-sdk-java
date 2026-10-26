// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import java.util.Base64;
import javax.crypto.SecretKey;

/**
 * One-shot encryption material for a single encrypted-channel request (J7): server public key +
 * freshly generated AES key + IV. {@link #keyHeader()} renders the {@code
 * X-DashScope-EncryptionKey} header value.
 */
public final class EncryptionConfig {
  private final String publicKeyId;
  private final String base64PublicKey;
  private final SecretKey aesKey;
  private final byte[] iv;

  public EncryptionConfig(String publicKeyId, String base64PublicKey, SecretKey aesKey, byte[] iv) {
    this.publicKeyId = publicKeyId;
    this.base64PublicKey = base64PublicKey;
    this.aesKey = aesKey;
    this.iv = iv;
  }

  public String getPublicKeyId() {
    return publicKeyId;
  }

  public String getBase64PublicKey() {
    return base64PublicKey;
  }

  public SecretKey getAesKey() {
    return aesKey;
  }

  public byte[] getIv() {
    return iv;
  }

  /** {@code <publicKeyId>,<RSA(base64(aesKey))>,<base64(iv)>} — 2.x header parity. */
  public String keyHeader() {
    String base64Key = Base64.getEncoder().encodeToString(aesKey.getEncoded());
    return publicKeyId
        + ","
        + EncryptionUtils.rsaEncrypt(base64Key, base64PublicKey)
        + ","
        + Base64.getEncoder().encodeToString(iv);
  }
}
