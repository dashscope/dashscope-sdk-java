// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.finetunes;

import com.alibaba.dashscope.transport.JsonSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Wire helpers for the fine-tunes plane. */
final class FineTunesSupport {
  private FineTunesSupport() {}

  /** Fine-tune jobs may report usage as a bare token count rather than the usual object. */
  static void normalizeUsage(ObjectNode flat) {
    JsonNode usage = flat.get("usage");
    if (usage != null && usage.isNumber()) {
      ObjectNode normalized = flat.putObject("usage");
      normalized.put("total_tokens", usage.asLong());
    }
  }
}
