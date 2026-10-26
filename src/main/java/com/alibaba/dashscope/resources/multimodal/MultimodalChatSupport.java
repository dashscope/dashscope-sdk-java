// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.multimodal;

import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCompletion;
import com.alibaba.dashscope.types.multimodal.MultimodalChatCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wire mapping for multimodal-generation: body building, envelope flattening, and the 2.x-parity
 * client-side merge that turns incremental frames into cumulative ones when the caller asked for
 * {@code incremental_output=false} on a model without server-side cumulative output.
 */
final class MultimodalChatSupport {
  private MultimodalChatSupport() {}

  static final String PATH = "/services/aigc/multimodal-generation/generation";
  /** Functional-area tag for x-dashscope-sdk-client (matches 2.x "aigc"). */
  static final String MODULE = "aigc";

  /**
   * 2.x ParamUtils parity: models containing tts/omni/qwen-deep-research manage their own output
   * mode; for everything else a {@code incremental_output=false} request is served by flipping the
   * wire value to incremental and merging client-side.
   */
  static boolean shouldModifyIncrementalOutput(String model) {
    if (model == null) {
      return true;
    }
    String lower = model.toLowerCase();
    return !(lower.contains("tts")
        || lower.contains("omni")
        || lower.contains("qwen-deep-research"));
  }

  static ObjectNode buildBody(
      MultimodalChatCreateParams params, boolean stream, boolean mergeIncremental) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());

    ObjectNode input = root.putObject("input");
    input.set("messages", mapper.valueToTree(params.getMessages()));
    if (params.getExtraInput() != null) {
      params.getExtraInput().forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }

    ObjectNode parameters = root.putObject("parameters");
    parameters.put("result_format", "message");
    if (mergeIncremental) {
      parameters.put("incremental_output", true);
    } else if (params.getIncrementalOutput() != null) {
      parameters.put("incremental_output", params.getIncrementalOutput());
    }
    putIfNotNull(parameters, "temperature", params.getTemperature());
    putIfNotNull(parameters, "top_p", params.getTopP());
    putIfNotNull(parameters, "top_k", params.getTopK());
    putIfNotNull(parameters, "max_tokens", params.getMaxTokens());
    putIfNotNull(parameters, "seed", params.getSeed());
    putIfNotNull(parameters, "repetition_penalty", params.getRepetitionPenalty());
    putIfNotNull(parameters, "enable_thinking", params.getEnableThinking());
    putIfNotNull(parameters, "thinking_budget", params.getThinkingBudget());
    putIfNotNull(parameters, "n", params.getN());
    putIfNotNull(parameters, "enable_search", params.getEnableSearch());
    putIfNotNull(parameters, "vl_high_resolution_images", params.getVlHighResolutionImages());
    if (params.getStop() != null) {
      ArrayNode stop = parameters.putArray("stop");
      params.getStop().forEach(stop::add);
    }
    if (params.getResponseFormat() != null) {
      parameters.set("response_format", mapper.valueToTree(params.getResponseFormat()));
    }
    if (params.getToolChoice() != null) {
      parameters.set("tool_choice", mapper.valueToTree(params.getToolChoice()));
    }
    if (params.getTools() != null) {
      parameters.set("tools", mapper.valueToTree(params.getTools()));
    }
    if (params.getSearchOptions() != null) {
      parameters.set("search_options", mapper.valueToTree(params.getSearchOptions()));
    }
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  static MultimodalChatCompletion mapCompletion(JsonNode body) {
    return JsonSupport.mapper().convertValue(flatten(body), MultimodalChatCompletion.class);
  }

  static MultimodalChatChunk mapChunk(JsonNode frame) {
    return JsonSupport.mapper().convertValue(frame, MultimodalChatChunk.class);
  }

  /**
   * Flatten the server envelope into the response type shape. Unlike chat completions, content
   * stays in list-of-parts form — that is the canonical multimodal shape.
   */
  static ObjectNode flatten(JsonNode body) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode flat = mapper.createObjectNode();
    if (body == null || !body.isObject()) {
      return flat;
    }
    if (body.hasNonNull("model")) {
      flat.set("model", body.get("model"));
    }
    if (body.hasNonNull("usage")) {
      flat.set("usage", body.get("usage"));
    }
    JsonNode output = body.get("output");
    if (output != null && output.isObject()) {
      JsonNode choices = output.get("choices");
      if (choices != null && choices.isArray()) {
        flat.set("choices", choices);
      } else if (output.hasNonNull("text")) {
        // Legacy result_format=text shape: synthesize a single canonical choice.
        ArrayNode normalized = flat.putArray("choices");
        ObjectNode choice = normalized.addObject();
        choice.put("index", 0);
        if (output.hasNonNull("finish_reason")) {
          choice.set("finish_reason", output.get("finish_reason"));
        }
        ObjectNode message = choice.putObject("message");
        message.put("role", "assistant");
        ArrayNode content = message.putArray("content");
        content.addObject().put("text", output.get("text").asText());
      }
      if (output.hasNonNull("search_info")) {
        flat.set("search_info", output.get("search_info"));
      }
    }
    return flat;
  }

  /**
   * Stateful per-stream merger (2.x MultiModalConversation.mergeSingleResponse parity): keyed by
   * choice loop index; text parts concat into one text item; reasoning_content appends; tool_calls
   * merge by index (name/arguments concat, output replace-latest, id replace-if-nonempty,
   * type/index replace). Each emitted frame carries the cumulative values.
   */
  static final class IncrementalMerger {
    private final Map<Integer, Acc> byChoice = new HashMap<>();

    ObjectNode merge(ObjectNode frame) {
      JsonNode choices = frame.get("choices");
      if (choices == null || !choices.isArray()) {
        return frame;
      }
      int loopIndex = 0;
      for (JsonNode choiceNode : choices) {
        if (choiceNode.isObject()) {
          Acc acc = byChoice.computeIfAbsent(loopIndex, k -> new Acc());
          JsonNode message = ((ObjectNode) choiceNode).get("message");
          if (message instanceof ObjectNode) {
            mergeContent((ObjectNode) message, acc);
            mergeReasoning((ObjectNode) message, acc);
            mergeToolCalls((ObjectNode) message, acc);
          }
        }
        loopIndex++;
      }
      return frame;
    }

    private static void mergeContent(ObjectNode message, Acc acc) {
      JsonNode content = message.get("content");
      if (content != null && content.isArray()) {
        for (JsonNode item : content) {
          JsonNode text = item instanceof ObjectNode ? item.get("text") : null;
          if (text != null && text.isTextual() && !text.asText().isEmpty()) {
            if (acc.text == null) {
              acc.text = new StringBuilder();
            }
            acc.text.append(text.asText());
          }
        }
      }
      if (acc.text != null) {
        ArrayNode merged = JsonSupport.mapper().createArrayNode();
        merged.addObject().put("text", acc.text.toString());
        message.set("content", merged);
      }
    }

    private static void mergeReasoning(ObjectNode message, Acc acc) {
      JsonNode reasoning = message.get("reasoning_content");
      if (reasoning != null && reasoning.isTextual() && !reasoning.asText().isEmpty()) {
        acc.reasoning.append(reasoning.asText());
      }
      if (acc.reasoning.length() > 0) {
        message.put("reasoning_content", acc.reasoning.toString());
      }
    }

    private static void mergeToolCalls(ObjectNode message, Acc acc) {
      JsonNode current = message.get("tool_calls");
      if (current != null && current.isArray()) {
        for (JsonNode node : current) {
          if (!(node instanceof ObjectNode)) {
            continue;
          }
          ObjectNode call = (ObjectNode) node;
          JsonNode indexNode = call.get("index");
          if (indexNode == null || !indexNode.isIntegralNumber()) {
            continue;
          }
          ObjectNode existing = findByIndex(acc.toolCalls, indexNode.asInt());
          if (existing == null) {
            acc.toolCalls.add(call.deepCopy());
            continue;
          }
          JsonNode currentFunction = call.get("function");
          if (currentFunction instanceof ObjectNode) {
            ObjectNode existingFunction =
                existing.get("function") instanceof ObjectNode
                    ? (ObjectNode) existing.get("function")
                    : existing.putObject("function");
            concatField(existingFunction, (ObjectNode) currentFunction, "arguments");
            concatField(existingFunction, (ObjectNode) currentFunction, "name");
            if (currentFunction.hasNonNull("output")) {
              existingFunction.set("output", currentFunction.get("output"));
            }
          }
          existing.set("index", call.get("index"));
          JsonNode id = call.get("id");
          if (id != null && id.isTextual() && !id.asText().isEmpty()) {
            existing.set("id", id);
          }
          if (call.hasNonNull("type")) {
            existing.set("type", call.get("type"));
          }
        }
      }
      if (!acc.toolCalls.isEmpty()) {
        ArrayNode merged = JsonSupport.mapper().createArrayNode();
        acc.toolCalls.forEach(merged::add);
        message.set("tool_calls", merged);
      }
    }

    private static ObjectNode findByIndex(List<ObjectNode> calls, int index) {
      for (ObjectNode call : calls) {
        JsonNode node = call.get("index");
        if (node != null && node.isIntegralNumber() && node.asInt() == index) {
          return call;
        }
      }
      return null;
    }

    private static void concatField(ObjectNode target, ObjectNode current, String field) {
      JsonNode value = current.get(field);
      if (value != null && value.isTextual()) {
        JsonNode existing = target.get(field);
        target.put(
            field,
            (existing != null && existing.isTextual() ? existing.asText() : "") + value.asText());
      }
    }

    private static final class Acc {
      StringBuilder text;
      final StringBuilder reasoning = new StringBuilder();
      final List<ObjectNode> toolCalls = new ArrayList<>();
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
