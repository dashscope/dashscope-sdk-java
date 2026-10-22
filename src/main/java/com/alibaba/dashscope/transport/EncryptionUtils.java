// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.transport;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Crypto primitives for the encrypted channel (J7, ported from 2.x utils/EncryptionUtils):
 * RSA-wrapped AES-256-GCM. Wire shape: the request carries an
 * {@code X-DashScope-EncryptionKey: <publicKeyId>,<RSA(base64(aesKey))>,<base64(iv)>} header and
 * the sensitive body part is AES-GCM encrypted.
 *
 * <p>Failure model: a JDK that lacks RSA/AES-GCM is a broken environment →
 * {@link IllegalStateException}; malformed caller input (bad key/iv/ciphertext) →
 * {@link IllegalArgumentException}.
 */
public final class EncryptionUtils {
  private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
  private static final int AES_KEY_LENGTH = 256;
  private static final int GCM_TAG_BITS = 128;
  private static final int IV_BYTES = 12;

  private EncryptionUtils() {}

  /** RSA-encrypt the input string; returns the base64 cipher text. */
  public static String rsaEncrypt(String input, String base64PublicKey) {
    try {
      Cipher cipher = Cipher.getInstance("RSA");
      byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, kf.generatePublic(new X509EncodedKeySpec(keyBytes)));
      return Base64.getEncoder().encodeToString(cipher.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException("JDK lacks RSA support", e);
    } catch (InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new IllegalArgumentException("RSA encryption failed: " + e.getMessage(), e);
    }
  }

  /** Generate a fresh AES-256 secret key. */
  public static SecretKey generateAesKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      keyGenerator.init(AES_KEY_LENGTH, new SecureRandom());
      return keyGenerator.generateKey();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JDK lacks AES support", e);
    }
  }

  /** Random 96-bit GCM IV. */
  public static byte[] generateIv() {
    byte[] iv = new byte[IV_BYTES];
    new SecureRandom().nextBytes(iv);
    return iv;
  }

  /** AES-GCM encrypt; returns the base64 cipher text. */
  public static String aesEncrypt(String input, SecretKey key, byte[] iv) {
    return aesEncrypt(input.getBytes(StandardCharsets.UTF_8), key, iv);
  }

  /** AES-GCM encrypt; returns the base64 cipher text. */
  public static String aesEncrypt(byte[] input, SecretKey key, byte[] iv) {
    try {
      Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      return Base64.getEncoder().encodeToString(cipher.doFinal(input));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException("JDK lacks AES-GCM support", e);
    } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new IllegalArgumentException("AES encryption failed: " + e.getMessage(), e);
    }
  }

  /** AES-GCM decrypt a base64 cipher text. */
  public static String aesDecrypt(String base64CipherText, SecretKey key, byte[] iv) {
    try {
      Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] plain = cipher.doFinal(Base64.getDecoder().decode(base64CipherText));
      return new String(plain, StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException("JDK lacks AES-GCM support", e);
    } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException
        | BadPaddingException e) {
      throw new IllegalArgumentException("AES decryption failed: " + e.getMessage(), e);
    }
  }
}
