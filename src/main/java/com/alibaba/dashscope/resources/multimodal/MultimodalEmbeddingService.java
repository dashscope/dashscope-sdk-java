// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.multimodal;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.oss.OssUploader;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.multimodal.MultimodalEmbeddingCreateParams;
import com.alibaba.dashscope.types.multimodal.MultimodalEmbeddingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;

/** {@code multimodal.embeddings} resource: {@code create}. */
public final class MultimodalEmbeddingService {
  static final String PATH = "/services/embeddings/multimodal-embedding/multimodal-embedding";
  static final String MODULE = "embeddings";

  private final HttpTransport transport;
  private final OssUploader uploader;

  public MultimodalEmbeddingService(HttpTransport transport) {
    this.transport = transport;
    this.uploader = new OssUploader(transport);
  }

  public MultimodalEmbeddingResponse create(MultimodalEmbeddingCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public MultimodalEmbeddingResponse create(
      MultimodalEmbeddingCreateParams params, RequestOptions options) {
    RequestOptions opts = options == null ? RequestOptions.NONE : options;
    MultimodalPreprocessor.ContentsResult pre =
        MultimodalPreprocessor.preprocessContents(
            params.getModel(), params.getContents(), uploader, opts);
    ResponseEnvelope envelope =
        transport.execute(
            PATH, buildBody(params, pre), MODULE, withOssResolve(opts, pre.ossResolve));
    MultimodalEmbeddingResponse response = mapResponse(envelope.getBody());
    response.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return response;
  }

  private static ObjectNode buildBody(
      MultimodalEmbeddingCreateParams params, MultimodalPreprocessor.ContentsResult pre) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    input.set("contents", mapper.valueToTree(pre.contents));
    if (params.getExtraInput() != null) {
      params.getExtraInput().forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    }
    if (params.getParameters() != null) {
      ObjectNode parameters = root.putObject("parameters");
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }

  private static MultimodalEmbeddingResponse mapResponse(JsonNode body) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode flat = mapper.createObjectNode();
    if (body != null && body.isObject()) {
      JsonNode output = body.get("output");
      if (output != null && output.isObject() && output.hasNonNull("embeddings")) {
        flat.set("embeddings", output.get("embeddings"));
      }
      if (body.hasNonNull("usage")) {
        flat.set("usage", body.get("usage"));
      }
    }
    return mapper.convertValue(flat, MultimodalEmbeddingResponse.class);
  }

  private static RequestOptions withOssResolve(RequestOptions options, boolean ossResolve) {
    if (!ossResolve) {
      return options;
    }
    Map<String, String> headers = new LinkedHashMap<>(options.getHeaders());
    headers.put(MultimodalPreprocessor.OSS_RESOLVE_HEADER, "enable");
    return RequestOptions.builder()
        .timeout(options.getTimeout())
        .maxRetries(options.getMaxRetries())
        .apiKey(options.getApiKey())
        .workspace(options.getWorkspace())
        .headers(headers)
        .build();
  }
}
