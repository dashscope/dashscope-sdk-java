// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.images;

import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.types.images.ImageGenerateParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/** Wire mapping for the two image protocols (Python v2 images.py parity). */
final class ImagesSupport {
  private ImagesSupport() {}

  /** Legacy text2image async-task endpoint (wan2.5 and below). */
  static final String SYNTHESIS_PATH = "/services/aigc/text2image/image-synthesis";
  /** Multimodal-protocol one-shot sync endpoint. */
  static final String MULTIMODAL_SYNC_PATH = "/services/aigc/multimodal-generation/generation";
  /** Multimodal-protocol async-task endpoint. */
  static final String MULTIMODAL_TASK_PATH = "/services/aigc/image-generation/generation";
  /** Functional-area tag for x-dashscope-sdk-client (matches 2.x "aigc"). */
  static final String MODULE = "aigc";

  static String taskPath(String model) {
    return ImageGenerateParams.usesMultimodalProtocol(model)
        ? MULTIMODAL_TASK_PATH
        : SYNTHESIS_PATH;
  }

  /** Legacy body: prompt input, negative_prompt in input. */
  static ObjectNode buildLegacyBody(
      ImageGenerateParams params, List<String> imageUrls, Map<String, Object> extraInput) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    input.put("prompt", params.getPrompt());
    if (params.getNegativePrompt() != null) {
      input.put("negative_prompt", params.getNegativePrompt());
    }
    mergeExtraInput(input, extraInput);
    if (params.getFunction() != null) {
      input.put("function", params.getFunction());
    }
    ObjectNode parameters = namedParameters(params, false);
    if (parameters != null) {
      root.set("parameters", parameters);
    }
    return root;
  }

  /** Multimodal body: messages input, negative_prompt in parameters. */
  static ObjectNode buildMultimodalBody(
      ImageGenerateParams params, List<String> imageUrls, Map<String, Object> extraInput) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    ArrayNode messages = input.putArray("messages");
    ObjectNode message = messages.addObject();
    message.put("role", "user");
    ArrayNode content = message.putArray("content");
    content.addObject().put("text", params.getPrompt());
    if (imageUrls != null) {
      for (String url : imageUrls) {
        content.addObject().put("image", url);
      }
    }
    mergeExtraInput(input, extraInput);
    if (params.getFunction() != null) {
      input.put("function", params.getFunction());
    }
    ObjectNode parameters = namedParameters(params, true);
    if (parameters != null) {
      root.set("parameters", parameters);
    }
    return root;
  }

  private static void mergeExtraInput(ObjectNode input, Map<String, Object> extraInput) {
    if (extraInput != null && !extraInput.isEmpty()) {
      ObjectMapper mapper = JsonSupport.mapper();
      extraInput.forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }
  }

  /** Named kwargs merged over {@code parameters} (named wins); null when the merge is empty. */
  private static ObjectNode namedParameters(
      ImageGenerateParams params, boolean negativePromptInParameters) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode merged = mapper.createObjectNode();
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> merged.set(k, mapper.valueToTree(v)));
    }
    if (negativePromptInParameters && params.getNegativePrompt() != null) {
      merged.put("negative_prompt", params.getNegativePrompt());
    }
    putIfNotNull(merged, "n", params.getN());
    if (params.getSize() != null) {
      merged.put("size", normalizeSize(params.getSize()));
    }
    putIfNotNull(merged, "seed", params.getSeed());
    putIfNotNull(merged, "style", params.getStyle());
    putIfNotNull(merged, "prompt_extend", params.getPromptExtend());
    putIfNotNull(merged, "watermark", params.getWatermark());
    return merged.isEmpty() ? null : merged;
  }

  /** Accept {@code 1024x1024} as well as the wire form {@code 1024*1024}. */
  static String normalizeSize(String size) {
    return size.replace('x', '*').replace('X', '*');
  }

  private static void putIfNotNull(ObjectNode node, String key, Integer value) {
    if (value != null) {
      node.put(key, value);
    }
  }

  private static void putIfNotNull(ObjectNode node, String key, String value) {
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
