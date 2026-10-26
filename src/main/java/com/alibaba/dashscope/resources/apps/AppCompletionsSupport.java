// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.apps;

import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.apps.AppCompletion;
import com.alibaba.dashscope.types.apps.AppCompletionCreateParams;
import com.alibaba.dashscope.types.messages.Message;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Iterator;
import java.util.Map;

/** Wire mapping for the Bailian application completion protocol: body building and flattening. */
final class AppCompletionsSupport {
  private AppCompletionsSupport() {}

  /** {@code POST /apps/{app_id}/completion}. */
  static final String PATH_PREFIX = "/apps/";

  static final String PATH_SUFFIX = "/completion";
  /** Functional-area tag for x-dashscope-sdk-client (matches 2.x "app"). */
  static final String MODULE = "app";

  static String pathOf(String appId) {
    return PATH_PREFIX + appId + PATH_SUFFIX;
  }

  static ObjectNode buildBody(AppCompletionCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();

    ObjectNode input = root.putObject("input");
    if (params.getMessages() == null) {
      input.put("prompt", params.getPrompt());
    } else {
      ArrayNode messages = input.putArray("messages");
      for (Message m : params.getMessages()) {
        messages.add(mapper.valueToTree(m));
      }
      // prompt + messages: prompt is appended as the latest user turn.
      if (params.getPrompt() != null && !params.getPrompt().isEmpty()) {
        messages.add(mapper.valueToTree(Message.user(params.getPrompt())));
      }
      if (params.getMessages().isEmpty() && params.getPrompt() == null) {
        // Unreachable: build() rejects prompt-and-messages both absent.
        throw new IllegalArgumentException("prompt or messages is required");
      }
    }
    putIfNotNull(input, "session_id", params.getSessionId());
    putIfNotNull(input, "doc_reference_type", params.getDocReferenceType());
    if (params.getDocTagCodes() != null) {
      ArrayNode codes = input.putArray("doc_tag_codes");
      params.getDocTagCodes().forEach(codes::add);
    }
    putIfNotNull(input, "memory_id", params.getMemoryId());
    if (params.getBizParams() != null && !params.getBizParams().isEmpty()) {
      input.set("biz_params", mapper.valueToTree(params.getBizParams()));
    }
    if (params.getImageList() != null && !params.getImageList().isEmpty()) {
      ArrayNode images = input.putArray("image_list");
      params.getImageList().forEach(images::add);
    }
    if (params.getFileList() != null && !params.getFileList().isEmpty()) {
      ArrayNode files = input.putArray("file_list");
      params.getFileList().forEach(files::add);
    }

    ObjectNode parameters = root.putObject("parameters");
    putIfNotNull(parameters, "temperature", params.getTemperature());
    putIfNotNull(parameters, "top_p", params.getTopP());
    putIfNotNull(parameters, "top_k", params.getTopK());
    putIfNotNull(parameters, "seed", params.getSeed());
    putIfNotNull(parameters, "has_thoughts", params.getHasThoughts());
    if (params.getRagOptions() != null) {
      parameters.set("rag_options", mapper.valueToTree(params.getRagOptions()));
    }
    putIfNotNull(parameters, "incremental_output", params.getIncrementalOutput());
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  static AppCompletion mapCompletion(JsonNode body) {
    return JsonSupport.mapper().convertValue(flatten(body), AppCompletion.class);
  }

  /**
   * Lift {@code output} to the top level; envelope keys win and null envelope values are dropped
   * (Python v2 {@code flatten_envelope} parity). The same shape serves non-streaming bodies and
   * streaming frames.
   */
  private static ObjectNode flatten(JsonNode body) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode flat = mapper.createObjectNode();
    if (body == null || !body.isObject()) {
      return flat;
    }
    JsonNode output = body.get("output");
    if (output != null && output.isObject()) {
      output.fields().forEachRemaining(e -> flat.set(e.getKey(), e.getValue()));
    }
    Iterator<Map.Entry<String, JsonNode>> fields = body.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> e = fields.next();
      if (!"output".equals(e.getKey()) && !e.getValue().isNull()) {
        flat.set(e.getKey(), e.getValue());
      }
    }
    return flat;
  }

  private static void putIfNotNull(ObjectNode node, String key, String value) {
    if (value != null) {
      node.put(key, value);
    }
  }

  private static void putIfNotNull(ObjectNode node, String key, Double value) {
    if (value != null) {
      node.put(key, value);
    }
  }

  private static void putIfNotNull(ObjectNode node, String key, Integer value) {
    if (value != null) {
      node.put(key, value);
    }
  }

  private static void putIfNotNull(ObjectNode node, String key, Boolean value) {
    if (value != null) {
      node.put(key, value);
    }
  }
}
