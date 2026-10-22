// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.rerank;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.types.rerank.RerankCreateParams;
import com.alibaba.dashscope.v3.types.rerank.RerankResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** {@code rerank} resource: {@code create}. */
public final class RerankService {
  static final String PATH = "/services/rerank/text-rerank/text-rerank";
  static final String MODULE = "rerank";

  private final HttpTransport transport;

  public RerankService(HttpTransport transport) {
    this.transport = transport;
  }

  public RerankResponse create(RerankCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public RerankResponse create(RerankCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope = transport.execute(PATH, buildBody(params), MODULE, options);
    RerankResponse response = mapResponse(envelope.getBody());
    response.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return response;
  }

  static ObjectNode buildBody(RerankCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    input.put("query", params.getQuery());
    ArrayNode documents = input.putArray("documents");
    params.getDocuments().forEach(documents::add);
    ObjectNode parameters = root.putObject("parameters");
    if (params.getTopN() != null) {
      parameters.put("top_n", params.getTopN());
    }
    if (params.getReturnDocuments() != null) {
      parameters.put("return_documents", params.getReturnDocuments());
    }
    if (params.getInstruct() != null) {
      parameters.put("instruct", params.getInstruct());
    }
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  static RerankResponse mapResponse(JsonNode body) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode flat = mapper.createObjectNode();
    if (body != null && body.isObject()) {
      if (body.hasNonNull("usage")) {
        flat.set("usage", body.get("usage"));
      }
      JsonNode output = body.get("output");
      if (output != null && output.isObject() && output.hasNonNull("results")) {
        flat.set("results", output.get("results"));
      }
    }
    return mapper.convertValue(flat, RerankResponse.class);
  }
}
