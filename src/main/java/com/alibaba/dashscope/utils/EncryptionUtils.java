package com.alibaba.dashscope.utils;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
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

public class EncryptionUtils {
  private static final String algorithm = "AES/GCM/NoPadding";
  private static final Integer keyLength = 256;

  /**
   * RSA encrypt the input string, return the base64 encoded cipher text.
   *
   * @param input The string to be encrypted.
   * @param base64AESEncryptKey The base64 encoded AES encrypt key.
   * @return The base64 encoded cipher of the input.
   * @throws ApiException
   */
  public static String RSAEncrypt(String input, String base64AESEncryptKey) throws ApiException {
    try {
      Cipher cipher = Cipher.getInstance("RSA");
      byte[] keyBytes = Base64.getDecoder().decode(base64AESEncryptKey);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      cipher.init(1, kf.generatePublic(spec));
      byte[] cipherBytes = cipher.doFinal(input.getBytes());
      return Base64.getEncoder().encodeToString(cipherBytes);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException
        | InvalidKeySpecException e) {
      throw new ApiException(e);
    }
  }

  /**
   * Generate a AES secret key.
   *
   * @return The AES SecretKey.
   */
  public static SecretKey generateAESKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
      SecureRandom secureRandom = new SecureRandom();
      keyGenerator.init(keyLength, secureRandom);
      SecretKey key = keyGenerator.generateKey();
      return key;
    } catch (NoSuchAlgorithmException e) {
      throw new ApiException(e);
    }
  }

  /**
   * AES encrypt input, and output base64 encoded cipher text.
   *
   * @param input The input string
   * @param secretKey The AES secret key.
   * @param iv The Initialization Vector (IV) to augment the encryption.
   * @return The base64 encoded cipher text.
   * @throws ApiException
   */
  public static String AESEncrypt(String input, SecretKey secretKey, byte[] iv)
      throws ApiException {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
      byte[] cipherInput = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(cipherInput);
    } catch (IllegalBlockSizeException
        | BadPaddingException
        | NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException e) {
      throw new ApiException(e);
    }
  }

  /**
   * AES encrypt input, and output base64 encoded cipher text.
   *
   * @param input The input bytes to encrypt.
   * @param secretKey The AES secret key.
   * @param iv The Initialization Vector (IV) to augment the encryption.
   * @return The base64 encoded cipher text.
   * @throws ApiException
   */
  public static String AESEncrypt(byte[] input, SecretKey secretKey, byte[] iv)
      throws ApiException {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
      byte[] cipherInput = cipher.doFinal(input);
      return Base64.getEncoder().encodeToString(cipherInput);
    } catch (IllegalBlockSizeException
        | BadPaddingException
        | NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | InvalidAlgorithmParameterException e) {
      throw new ApiException(e);
    }
  }

  /**
   * AES decrypting encrypted cipher text.
   *
   * @param cipherText The cipher text to be decrypted.
   * @param secretKey The secret key to use.
   * @param iv The Initialization Vector (IV) of the encryption.
   * @return The plain text of cipher text.
   * @throws ApiException
   */
  public static String AESDecrypt(String cipherText, SecretKey secretKey, byte[] iv)
      throws ApiException {
    try {
      Cipher cipher = Cipher.getInstance(algorithm);
      GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);
      byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
      return new String(plainText, StandardCharsets.UTF_8);
    } catch (NoSuchAlgorithmException
        | NoSuchPaddingException
        | InvalidKeyException
        | IllegalBlockSizeException
        | BadPaddingException
        | InvalidAlgorithmParameterException e) {
      throw new ApiException(e);
    }
  }

  /** Generate a EncryptionConfig. */
  public static EncryptionConfig generateEncryptionConfig(String apiKey)
      throws ApiException, NoApiKeyException {
    EncryptionKeys encryptionKeys = new EncryptionKeys();
    EncryptionKey encryptionKey = encryptionKeys.get(apiKey);
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);
    EncryptionConfig encryptionConfig =
        EncryptionConfig.builder()
            .base64PublicKey(encryptionKey.getPublicKey())
            .publicKeyId(encryptionKey.getPublicKeyId())
            .AESEncryptKey(EncryptionUtils.generateAESKey())
            .iv(iv)
            .build();
    return encryptionConfig;
  }
}
