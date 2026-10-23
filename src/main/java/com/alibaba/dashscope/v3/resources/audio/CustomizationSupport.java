// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.audio;

import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

/**
 * Shared helpers for the audio customization envelope (audio design §1.4): every verb is one POST
 * carrying {@code {model, input: {action, ...}, parameters?}}; only the action words and id fields
 * differ per family. Also shared by {@code phrases} (same envelope flattening).
 */
final class CustomizationSupport {
  static final String MODULE = "audio";

  private CustomizationSupport() {}

  static ObjectNode body(
      String defaultModel,
      String model,
      String action,
      Map<String, Object> fields,
      Map<String, Object> parameters) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", model != null && !model.isEmpty() ? model : defaultModel);
    ObjectNode input = root.putObject("input");
    if (fields != null) {
      fields.forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }
    input.put("action", action);
    if (parameters != null) {
      ObjectNode node = root.putObject("parameters");
      parameters.forEach((k, v) -> node.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  static <T extends ResponseBase> T parse(ResponseEnvelope envelope, Class<T> type) {
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    T result = JsonSupport.mapper().convertValue(flat, type);
    fillFromFlat(result, flat, envelope);
    return result;
  }

  static void fillFromFlat(ResponseBase result, ObjectNode flat, ResponseEnvelope envelope) {
    String requestId = textOrNull(flat.get("request_id"));
    if (requestId == null) {
      requestId = JsonSupport.requestId(null, envelope.getHeaders());
    }
    result.fillEnvelope(
        requestId,
        envelope.getStatusCode(),
        textOrNull(flat.get("code")),
        textOrNull(flat.get("message")),
        envelope.getHeaders());
  }

  static String requireId(String value, String name) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " must be a non-empty string");
    }
    return value;
  }

  static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
