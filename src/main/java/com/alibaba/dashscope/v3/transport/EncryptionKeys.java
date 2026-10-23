// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.transport;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.v3.errors.Errors;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Fetches the server's latest encryption public key (J7): {@code GET /public-keys/latest} → {@code
 * {request_id, data:{public_key, public_key_id}}}, then bundles it with fresh AES material into an
 * {@link EncryptionConfig}.
 */
public final class EncryptionKeys {
  static final String PATH = "/public-keys/latest";
  static final String MODULE = "utils";

  private final HttpTransport transport;

  public EncryptionKeys(HttpTransport transport) {
    this.transport = transport;
  }

  /** Fetch the latest public key and build a fresh {@link EncryptionConfig}. */
  public EncryptionConfig createConfig(RequestOptions options) {
    ResponseEnvelope envelope = transport.executeGet(PATH, null, MODULE, options);
    JsonNode data = envelope.getBody() == null ? null : envelope.getBody().get("data");
    if (data == null || !data.isObject()) {
      PublicErrorCode entry = PublicErrorCode.RESPONSE_ERROR;
      throw Errors.fromStatus(
          entry.getStatusCode(),
          entry.getErrorCode(),
          "public-keys response carries no data object",
          JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
          JsonSupport.toPlainValue(envelope.getBody()),
          envelope.getHeaders());
    }
    return new EncryptionConfig(
        text(data.get("public_key_id")),
        text(data.get("public_key")),
        EncryptionUtils.generateAesKey(),
        EncryptionUtils.generateIv());
  }

  private static String text(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
