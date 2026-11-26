// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import javax.crypto.SecretKey;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

/** Encryption material for an encrypted AgentStudio request. */
@Data
@SuperBuilder
public class EncryptionConfig {
  @NotNull private String publicKeyId;
  @NotNull private String base64PublicKey;
  @NotNull private SecretKey AESEncryptKey;

  /** default iv. */
  @Default private byte[] iv = "000000000000".getBytes();
}
