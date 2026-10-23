// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.transport;

import com.alibaba.dashscope.common.PublicErrorCode;
import com.alibaba.dashscope.v3.errors.Errors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Shared Jackson mapper and wire-envelope helpers. Single mapper instance per JVM. */
public final class JsonSupport {
  private JsonSupport() {}

  // Field visibility: public models are immutable-with-getters, so (de)serialization goes
  // through fields directly. FAIL_ON_UNKNOWN_PROPERTIES=false for forward compatibility
  // (design §4).
  private static final ObjectMapper MAPPER =
      new ObjectMapper()
          .setVisibility(
              com.fasterxml.jackson.annotation.PropertyAccessor.FIELD,
              com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY)
          .configure(
              com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
              false);

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public static JsonNode readTree(String text) {
    try {
      return MAPPER.readTree(text);
    } catch (IOException e) {
      // Catalog-sourced, never an ad-hoc code: PublicErrorCode.RESPONSE_ERROR is exactly
      // "the server returned a response that could not be parsed". Route through the same
      // Errors factory every other error uses.
      PublicErrorCode entry = PublicErrorCode.RESPONSE_ERROR;
      throw Errors.fromStatus(
          entry.getStatusCode(),
          entry.getErrorCode(),
          "failed to parse response JSON: " + e.getMessage(),
          null,
          null,
          null);
    }
  }

  /**
   * Parse an error response body, preferring a plain JSON object; falls back to extracting the
   * error frame from SSE-shaped text (older gateways answer streaming requests with SSE frames even
   * on failure). Never throws: unparseable bodies yield null.
   */
  public static JsonNode parseErrorBody(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.readTree(text);
    } catch (IOException notJson) {
      return sseErrorBody(text);
    }
  }

  /** Port of Python v2 _sse_error_body: prefer frames carrying a code, else a message-only one. */
  private static JsonNode sseErrorBody(String text) {
    List<String> dataLines = new ArrayList<>();
    JsonNode fallback = null;
    for (String line : text.split("\n")) {
      if (line.startsWith("data:")) {
        dataLines.add(line.substring("data:".length()));
      } else if (line.equals("data")) {
        dataLines.add("");
      } else if (line.trim().isEmpty()) {
        JsonNode found = flush(dataLines);
        if (found != null && isErrorFrame(found)) {
          return found;
        }
        if (found != null && fallback == null && found.hasNonNull("message")) {
          fallback = found;
        }
        dataLines.clear();
      }
    }
    JsonNode found = flush(dataLines);
    if (found != null && isErrorFrame(found)) {
      return found;
    }
    return found != null ? found : fallback;
  }

  private static JsonNode flush(List<String> dataLines) {
    if (dataLines.isEmpty()) {
      return null;
    }
    String joined = String.join("\n", dataLines);
    try {
      return MAPPER.readTree(joined);
    } catch (IOException e) {
      // Older gateways omit blank lines between complete JSON frames; try line by line.
      for (String line : dataLines) {
        try {
          JsonNode node = MAPPER.readTree(line);
          if (node != null && node.isObject()) {
            return node;
          }
        } catch (IOException ignoredLine) {
          // not a standalone JSON frame; keep scanning the remaining lines
        }
      }
      return null;
    }
  }

  /**
   * A frame is an error frame when it carries a top-level "code" or a nested "error" object with
   * code/type/message.
   */
  public static boolean isErrorFrame(JsonNode body) {
    if (body == null || !body.isObject()) {
      return false;
    }
    if (body.hasNonNull("code") && !body.get("code").asText().isEmpty()) {
      return true;
    }
    JsonNode error = body.get("error");
    if (error != null && error.isObject()) {
      return error.hasNonNull("code") || error.hasNonNull("type") || error.hasNonNull("message");
    }
    return false;
  }

  /** Wire error code: top-level "code", else nested error.code / error.type. */
  public static String errorCode(JsonNode body) {
    if (body == null || !body.isObject()) {
      return null;
    }
    JsonNode code = body.get("code");
    if (code != null && code.isTextual() && !code.asText().isEmpty()) {
      return code.asText();
    }
    JsonNode error = body.get("error");
    if (error != null && error.isObject()) {
      JsonNode c = error.get("code");
      if (c != null && c.isTextual()) {
        return c.asText();
      }
      JsonNode t = error.get("type");
      if (t != null && t.isTextual()) {
        return t.asText();
      }
    }
    return null;
  }

  /** Wire error message: top-level "message", else nested error.message. */
  public static String errorMessage(JsonNode body) {
    if (body == null || !body.isObject()) {
      return null;
    }
    JsonNode message = body.get("message");
    if (message != null && message.isTextual() && !message.asText().isEmpty()) {
      return message.asText();
    }
    JsonNode error = body.get("error");
    if (error != null && error.isObject()) {
      JsonNode m = error.get("message");
      if (m != null && m.isTextual()) {
        return m.asText();
      }
    }
    return null;
  }

  /** request_id from the body, else the x-request-id header. */
  public static String requestId(JsonNode body, Map<String, String> headers) {
    if (body != null && body.isObject()) {
      JsonNode id = body.get("request_id");
      if (id != null && id.isTextual() && !id.asText().isEmpty()) {
        return id.asText();
      }
    }
    if (headers != null) {
      for (Map.Entry<String, String> e : headers.entrySet()) {
        if ("x-request-id".equalsIgnoreCase(e.getKey())) {
          return e.getValue();
        }
      }
    }
    return null;
  }

  /** Convert a JsonNode to plain Java values (Map/List/scalars) for exception bodies. */
  public static Object toPlainValue(JsonNode node) {
    if (node == null) {
      return null;
    }
    return MAPPER.convertValue(node, Object.class);
  }

  /**
   * Lift {@code body.output} to the top level so public types stay flat (Python v2 {@code
   * flatten_envelope} parity). Envelope keys win — how {@code request_id}/{@code code}/{@code
   * message} reach the flattened node; null envelope values are dropped so an absent field cannot
   * overwrite a real payload value.
   */
  public static com.fasterxml.jackson.databind.node.ObjectNode flattenEnvelope(JsonNode body) {
    com.fasterxml.jackson.databind.node.ObjectNode flat = MAPPER.createObjectNode();
    if (body == null || !body.isObject()) {
      return flat;
    }
    JsonNode output = body.get("output");
    if (output != null && output.isObject()) {
      flat.setAll((com.fasterxml.jackson.databind.node.ObjectNode) output);
    }
    body.fields()
        .forEachRemaining(
            e -> {
              if (!"output".equals(e.getKey()) && !e.getValue().isNull()) {
                flat.set(e.getKey(), e.getValue());
              }
            });
    return flat;
  }

  public static JsonNode missingNode() {
    return JsonNodeFactory.instance.missingNode();
  }

  /** Shallow iterable over array elements; empty when the node is not an array. */
  public static Iterable<JsonNode> arrayElements(JsonNode node) {
    return () ->
        (node != null && node.isArray())
            ? node.elements()
            : new Iterator<JsonNode>() {
              @Override
              public boolean hasNext() {
                return false;
              }

              @Override
              public JsonNode next() {
                throw new java.util.NoSuchElementException();
              }
            };
  }
}
