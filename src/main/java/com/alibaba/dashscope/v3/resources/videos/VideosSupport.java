// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.videos;

import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.types.videos.VideoGenerateParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/** Wire mapping for the video-synthesis service (Python v2 videos.py parity). */
final class VideosSupport {
  private VideosSupport() {}

  static final String PATH = "/services/aigc/video-generation/video-synthesis";
  /** Functional-area tag for x-dashscope-sdk-client (matches 2.x "aigc"). */
  static final String MODULE = "aigc";

  static ObjectNode buildBody(
      VideoGenerateParams params,
      String imageUrl,
      List<Map<String, Object>> media,
      Map<String, Object> extraInput) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    root.set("input", buildInput(params, imageUrl, media, extraInput));
    ObjectNode parameters = namedParameters(params);
    if (parameters != null) {
      root.set("parameters", parameters);
    }
    return root;
  }

  private static ObjectNode buildInput(
      VideoGenerateParams params,
      String imageUrl,
      List<Map<String, Object>> media,
      Map<String, Object> extraInput) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode input = mapper.createObjectNode();
    if (params.getPrompt() != null) {
      input.put("prompt", params.getPrompt());
    }
    if (VideoGenerateParams.usesMediaProtocol(params.getModel())) {
      ArrayNode items = input.putArray("media");
      if (imageUrl != null) {
        // image_url is shorthand for a lone first_frame asset.
        items.addObject().put("type", "first_frame").put("url", imageUrl);
      }
      if (media != null) {
        for (Map<String, Object> item : media) {
          items.add(mapper.valueToTree(item));
        }
      }
    } else {
      if (media != null && !media.isEmpty()) {
        ArrayNode items = input.putArray("media");
        for (Map<String, Object> item : media) {
          items.add(mapper.valueToTree(item));
        }
      }
      if (imageUrl != null) {
        // Wire spelling; the public name is imageUrl.
        input.put("img_url", imageUrl);
      }
    }
    if (extraInput != null && !extraInput.isEmpty()) {
      extraInput.forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }
    if (params.getFunction() != null) {
      input.put("function", params.getFunction());
    }
    return input;
  }

  /** Named kwargs merged over {@code parameters} (named wins); null when the merge is empty. */
  private static ObjectNode namedParameters(VideoGenerateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode merged = mapper.createObjectNode();
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> merged.set(k, mapper.valueToTree(v)));
    }
    putIfNotNull(merged, "seed", params.getSeed());
    putIfNotNull(merged, "size", params.getSize());
    putIfNotNull(merged, "duration", params.getDuration());
    putIfNotNull(merged, "resolution", params.getResolution());
    putIfNotNull(merged, "ratio", params.getRatio());
    putIfNotNull(merged, "prompt_extend", params.getPromptExtend());
    putIfNotNull(merged, "watermark", params.getWatermark());
    return merged.isEmpty() ? null : merged;
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
