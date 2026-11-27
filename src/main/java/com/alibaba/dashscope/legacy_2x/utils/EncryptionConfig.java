package com.alibaba.dashscope.legacy_2x.utils;

import javax.crypto.SecretKey;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

@Data
@SuperBuilder
@Deprecated
public class EncryptionConfig {
  @NotNull private String publicKeyId;
  @NotNull private String base64PublicKey;
  @NotNull private SecretKey AESEncryptKey;

  /** default iv. */
  @Default private byte[] iv = "000000000000".getBytes();
}
