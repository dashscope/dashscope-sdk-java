// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.utils;

import com.alibaba.dashscope.applications.agentstudio.common.DashScopeResult;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Public key returned by the public-keys endpoint. */
@Data
public class EncryptionKey {
  @SerializedName("request_id")
  private String requestId;

  /*
   * base64 encoded public key
   */
  @SerializedName("public_key")
  private String publicKey;

  @SerializedName("public_key_id")
  private String publicKeyId;

  private EncryptionKey() {}

  public static EncryptionKey fromDashScopeResult(DashScopeResult result) {
    JsonObject dashscopeResult = (JsonObject) result.getOutput();
    EncryptionKey encryptionKey =
        JsonUtils.fromJsonObject(dashscopeResult.get("data"), EncryptionKey.class);
    encryptionKey.setRequestId(dashscopeResult.get("request_id").getAsString());
    encryptionKey.setPublicKey(encryptionKey.getPublicKey());
    return encryptionKey;
  }
}
