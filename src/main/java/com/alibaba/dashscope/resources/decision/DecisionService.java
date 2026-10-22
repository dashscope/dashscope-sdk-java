// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.decision;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.config.ResolvedConfig;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.decision.DecisionCreateParams;
import com.alibaba.dashscope.types.decision.DecisionQuestion;
import com.alibaba.dashscope.types.decision.DecisionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

/**
 * {@code decision} resource (TypeSafe System One decision model, design §3). One round trip, no
 * streaming: {@code POST {compatible-mode-root}/systemone}.
 *
 * <p>The endpoint sits under {@code /compatible-mode/v1} on every gateway, independent of the
 * native {@code /api/v1} root the rest of the SDK uses, so this resource derives its own base:
 * a resolved base URL ending in {@code /api/v1} (every default resolution does) swaps the suffix
 * for {@code /compatible-mode/v1}; any other base URL (explicit custom root, test doubles) is
 * used as-is.
 */
public final class DecisionService {
  static final String PATH = "/systemone";
  /** Functional-area tag for x-dashscope-sdk-client (matches 2.x "decision"). */
  static final String MODULE = "decision";

  private static final String NATIVE_SUFFIX = "/api/v1";
  private static final String COMPATIBLE_SUFFIX = "/compatible-mode/v1";

  private final HttpTransport transport;

  public DecisionService(HttpTransport transport) {
    this.transport = transport;
  }

  public DecisionResult create(DecisionCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public DecisionResult create(DecisionCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope =
        transport.execute(endpoint(transport.config()), buildBody(params), MODULE, options);
    DecisionResult result =
        JsonSupport.mapper().convertValue(
            envelope.getBody() == null ? JsonSupport.missingNode() : envelope.getBody(),
            DecisionResult.class);
    result.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
    return result;
  }

  static String endpoint(ResolvedConfig config) {
    String base = config.getBaseUrl();
    if (base.endsWith(NATIVE_SUFFIX)) {
      return base.substring(0, base.length() - NATIVE_SUFFIX.length()) + COMPATIBLE_SUFFIX + PATH;
    }
    return base + PATH;
  }

  private static ObjectNode buildBody(DecisionCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    root.set("state", mapper.valueToTree(params.getState()));
    ObjectNode questions = root.putObject("questions");
    for (Map.Entry<String, DecisionQuestion> e : params.getQuestions().entrySet()) {
      questions.set(e.getKey(), questionWire(mapper, e.getValue()));
    }
    return root;
  }

  private static JsonNode questionWire(ObjectMapper mapper, DecisionQuestion question) {
    if (question.isRaw()) {
      return mapper.valueToTree(question.getRaw());
    }
    ObjectNode wire = mapper.createObjectNode();
    wire.put("type", question.getType());
    if (question.getInstructions() != null) {
      wire.put("instructions", question.getInstructions());
    }
    if (question.getCriteria() != null) {
      wire.set("criteria", mapper.valueToTree(question.getCriteria()));
    }
    return wire;
  }
}
