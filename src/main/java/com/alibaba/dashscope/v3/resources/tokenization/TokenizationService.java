// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.tokenization;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.types.messages.Message;
import com.alibaba.dashscope.v3.types.tokenization.TokenCount;
import com.alibaba.dashscope.v3.types.tokenization.TokenizationCountParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * {@code tokenization} resource (Python v2 tokenization.py parity): server-side token counting over
 * {@code POST /tokenizer}. For the local BPE tokenizer see {@link
 * com.alibaba.dashscope.v3.tokenization.TokenizerFactory}.
 */
public final class TokenizationService {
  static final String PATH = "/tokenizer";
  static final String MODULE = "tokenizers";

  private final HttpTransport transport;

  public TokenizationService(HttpTransport transport) {
    this.transport = transport;
  }

  public TokenCount count(TokenizationCountParams params) {
    return count(params, RequestOptions.NONE);
  }

  public TokenCount count(TokenizationCountParams params, RequestOptions options) {
    ResponseEnvelope envelope = transport.execute(PATH, buildBody(params), MODULE, options);
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    TokenCount result = JsonSupport.mapper().convertValue(flat, TokenCount.class);
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
    return result;
  }

  static ObjectNode buildBody(TokenizationCountParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    boolean hasMessages = params.getMessages() != null && !params.getMessages().isEmpty();
    boolean hasPrompt = params.getPrompt() != null && !params.getPrompt().isEmpty();
    if (hasMessages) {
      ArrayNode messages = input.putArray("messages");
      for (Message message : params.getMessages()) {
        messages.add(mapper.valueToTree(message));
      }
      if (hasPrompt) {
        ObjectNode appended = messages.addObject();
        appended.put("role", "user");
        appended.put("content", params.getPrompt());
      }
    } else {
      input.put("prompt", params.getPrompt());
    }
    ObjectNode parameters = root.putObject("parameters");
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
