// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.chat;

import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.types.completions.ChatCompletion;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionChunk;
import com.alibaba.dashscope.v3.types.completions.ChatCompletionCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Wire mapping for the native chat completions protocol: body building and envelope flattening. */
final class ChatCompletionsSupport {
  private ChatCompletionsSupport() {}

  static final String PATH = "/services/aigc/text-generation/generation";
  /** Functional-area tag for x-dashscope-sdk-client (matches 2.x "aigc"). */
  static final String MODULE = "aigc";

  static ObjectNode buildBody(ChatCompletionCreateParams params, boolean stream) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());

    ObjectNode input = root.putObject("input");
    input.set("messages", mapper.valueToTree(params.getMessages()));
    if (params.getExtraInput() != null) {
      params.getExtraInput().forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }

    ObjectNode parameters = root.putObject("parameters");
    // 3.0 always speaks the message-shaped protocol; streaming asks for incremental frames.
    parameters.put("result_format", "message");
    if (stream) {
      parameters.put("incremental_output",
          params.getIncrementalOutput() == null || params.getIncrementalOutput());
    } else if (params.getIncrementalOutput() != null) {
      parameters.put("incremental_output", params.getIncrementalOutput());
    }
    putIfNotNull(parameters, "temperature", params.getTemperature());
    putIfNotNull(parameters, "top_p", params.getTopP());
    putIfNotNull(parameters, "max_tokens", params.getMaxTokens());
    putIfNotNull(parameters, "seed", params.getSeed());
    putIfNotNull(parameters, "repetition_penalty", params.getRepetitionPenalty());
    putIfNotNull(parameters, "presence_penalty", params.getPresencePenalty());
    putIfNotNull(parameters, "enable_thinking", params.getEnableThinking());
    putIfNotNull(parameters, "top_k", params.getTopK());
    putIfNotNull(parameters, "n", params.getN());
    putIfNotNull(parameters, "logprobs", params.getLogprobs());
    putIfNotNull(parameters, "top_logprobs", params.getTopLogprobs());
    putIfNotNull(parameters, "thinking_budget", params.getThinkingBudget());
    putIfNotNull(parameters, "enable_search", params.getEnableSearch());
    putIfNotNull(parameters, "parallel_tool_calls", params.getParallelToolCalls());
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

  static ChatCompletion mapCompletion(JsonNode body) {
    return JsonSupport.mapper().convertValue(flatten(body, false), ChatCompletion.class);
  }

  static ChatCompletionChunk mapChunk(JsonNode frame) {
    return JsonSupport.mapper().convertValue(flatten(frame, true), ChatCompletionChunk.class);
  }

  /** Flatten the server envelope into the response type shape: choices/usage/search_info. */
  private static ObjectNode flatten(JsonNode body, boolean streaming) {
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
        ArrayNode normalized = flat.putArray("choices");
        for (JsonNode choice : choices) {
          normalized.add(normalizeChoice((ObjectNode) choice, streaming));
        }
      } else if (output.hasNonNull("text")) {
        // Legacy result_format=text shape: synthesize a single canonical choice.
        ArrayNode normalized = flat.putArray("choices");
        ObjectNode choice = normalized.addObject();
        choice.put("index", 0);
        if (output.hasNonNull("finish_reason")) {
          choice.set("finish_reason", output.get("finish_reason"));
        }
        ObjectNode message = choice.putObject(streaming ? "delta" : "message");
        message.put("role", "assistant");
        message.put("content", flattenContent(output.get("text")));
      }
      if (output.hasNonNull("search_info")) {
        flat.set("search_info", output.get("search_info"));
      }
    }
    return flat;
  }

  private static ObjectNode normalizeChoice(ObjectNode choice, boolean streaming) {
    ObjectNode out = choice.deepCopy();
    if (streaming && out.has("message")) {
      // Native stream frames carry "message"; the canonical chunk shape calls it "delta".
      out.set("delta", out.remove("message"));
    }
    JsonNode holder = out.get(streaming ? "delta" : "message");
    if (holder != null && holder.isObject()) {
      JsonNode content = holder.get("content");
      if (content != null && content.isArray()) {
        // Multimodal endpoints return list-form content; join text parts so the
        // canonical model stays string-typed.
        ((ObjectNode) holder).put("content", flattenContent(content));
      }
    }
    return out;
  }

  private static String flattenContent(JsonNode content) {
    if (content == null || content.isNull()) {
      return null;
    }
    if (content.isTextual()) {
      return content.asText();
    }
    if (content.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode part : content) {
        JsonNode text = part.get("text");
        if (text != null && text.isTextual()) {
          sb.append(text.asText());
        }
      }
      return sb.toString();
    }
    return content.toString();
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
