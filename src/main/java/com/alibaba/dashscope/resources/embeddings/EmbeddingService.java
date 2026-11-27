// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.embeddings;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.embeddings.EmbeddingCreateParams;
import com.alibaba.dashscope.types.embeddings.EmbeddingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** {@code embeddings} resource: {@code create} plus the async batch-task facade {@code jobs()}. */
public final class EmbeddingService {
  static final String PATH = "/services/embeddings/text-embedding/text-embedding";
  static final String MODULE = "embeddings";

  private final HttpTransport transport;
  private final EmbeddingJobsService jobs;

  public EmbeddingService(HttpTransport transport) {
    this.transport = transport;
    this.jobs = new EmbeddingJobsService(transport);
  }

  /** The async batch-task handle: create/fetch/wait/cancel/list. */
  public EmbeddingJobsService jobs() {
    return jobs;
  }

  public EmbeddingResponse create(EmbeddingCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public EmbeddingResponse create(EmbeddingCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope = transport.execute(PATH, buildBody(params), MODULE, options);
    EmbeddingResponse response = mapResponse(envelope.getBody());
    response.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return response;
  }

  static ObjectNode buildBody(EmbeddingCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    ArrayNode texts = input.putArray("texts");
    params.getTexts().forEach(texts::add);
    ObjectNode parameters = root.putObject("parameters");
    if (params.getTextType() != null) {
      parameters.put("text_type", params.getTextType());
    }
    if (params.getDimension() != null) {
      parameters.put("dimension", params.getDimension());
    }
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  static EmbeddingResponse mapResponse(JsonNode body) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode flat = mapper.createObjectNode();
    if (body != null && body.isObject()) {
      if (body.hasNonNull("usage")) {
        flat.set("usage", body.get("usage"));
      }
      JsonNode output = body.get("output");
      if (output != null && output.isObject()) {
        JsonNode embeddings = output.get("embeddings");
        if (embeddings != null && embeddings.isArray()) {
          flat.set("embeddings", embeddings);
        } else if (output.hasNonNull("embedding")) {
          // multimodal-embedding-one-peace-v1 fuses content into one vector returned as
          // output.embedding; surface it as item 0 of the canonical list (Python parity).
          ArrayNode list = flat.putArray("embeddings");
          ObjectNode item = list.addObject();
          item.put("index", 0);
          item.set("embedding", output.get("embedding"));
        }
      }
    }
    return mapper.convertValue(flat, EmbeddingResponse.class);
  }
}
