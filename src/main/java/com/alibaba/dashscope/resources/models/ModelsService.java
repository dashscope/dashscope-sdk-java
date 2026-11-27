// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.models;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.errors.Errors;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.transport.ResponseEnvelope;
import com.alibaba.dashscope.types.models.Model;
import com.alibaba.dashscope.types.models.ModelList;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code models} resource (design §3, Python v2 models.py parity): list/get over {@code /models}.
 * {@code get} is the same endpoint filtered by query params; {@code provider} filters client-side
 * per page because the server has no such filter.
 */
public final class ModelsService {
  static final String BASE_PATH = "/models";
  static final String MODULE = "models";

  // Gateway wire code for an unknown model; routes through the Errors crosswalk
  // (Model.NotExist -> RESOURCE_NOT_FOUND -> NotFoundException).
  private static final String WIRE_MODEL_NOT_EXIST = "Model.NotExist";

  private final HttpTransport transport;

  public ModelsService(HttpTransport transport) {
    this.transport = transport;
  }

  public ModelList list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, null, RequestOptions.NONE);
  }

  public ModelList list(int pageNo, int pageSize, String provider, RequestOptions options) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page_no", String.valueOf(pageNo));
    query.put("page_size", String.valueOf(pageSize));
    ResponseEnvelope envelope = transport.executeGet(BASE_PATH, query, MODULE, options);
    ModelList result = parseList(envelope.getBody());
    if (provider != null && !provider.isEmpty()) {
      List<Model> filtered = new ArrayList<>();
      for (Model model : result.getModels()) {
        if (matchProvider(model, provider)) {
          filtered.add(model);
        }
      }
      // totalCount stays the pre-filter total of the whole catalog (Python parity).
      result =
          new ModelList(result.getPageNo(), result.getPageSize(), result.getTotalCount(), filtered);
    }
    fillEnvelope(result, envelope);
    return result;
  }

  public Model get(String model) {
    return get(model, RequestOptions.NONE);
  }

  public Model get(String model, RequestOptions options) {
    // Server filters by model name via query params.
    Map<String, String> query = new LinkedHashMap<>();
    query.put("model", model);
    query.put("page_no", "1");
    query.put("page_size", "1");
    ResponseEnvelope envelope = transport.executeGet(BASE_PATH, query, MODULE, options);
    List<Model> models = parseModels(envelope.getBody());
    if (models.isEmpty()) {
      if (transport.config().isRaiseOnError()) {
        throw Errors.fromBusinessCode(
            WIRE_MODEL_NOT_EXIST,
            "Model '" + model + "' not found",
            JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
            JsonSupport.toPlainValue(envelope.getBody()),
            envelope.getHeaders());
      }
      Model empty = new Model();
      fillEnvelope(empty, envelope);
      return empty;
    }
    Model result = models.get(0);
    fillEnvelope(result, envelope);
    return result;
  }

  private static boolean matchProvider(Model model, String provider) {
    String wanted = provider.toLowerCase(java.util.Locale.ROOT);
    return (model.getProvider() != null
            && model.getProvider().toLowerCase(java.util.Locale.ROOT).equals(wanted))
        || (model.getSource() != null
            && model.getSource().toLowerCase(java.util.Locale.ROOT).equals(wanted));
  }

  // The native envelope nests its page under "output"; "data" stays accepted for gateways that
  // re-wrap the page OpenAI-style.
  private static JsonNode dataOf(JsonNode body) {
    if (body == null) {
      return null;
    }
    JsonNode output = body.get("output");
    return output != null ? output : body.get("data");
  }

  private static ModelList parseList(JsonNode body) {
    JsonNode data = dataOf(body);
    if (data != null && data.isObject()) {
      List<Model> models = convertModels(data.get("models"));
      return new ModelList(
          intOr(data.get("page_no"), 1),
          intOr(data.get("page_size"), 10),
          intOr(data.get("total"), models.size()),
          models);
    }
    if (data != null && data.isArray()) {
      List<Model> models = convertModels(data);
      return new ModelList(1, 10, models.size(), models);
    }
    return new ModelList(1, 10, 0, null);
  }

  private static List<Model> parseModels(JsonNode body) {
    JsonNode data = dataOf(body);
    if (data != null && data.isObject()) {
      return convertModels(data.get("models"));
    }
    return new ArrayList<>();
  }

  private static List<Model> convertModels(JsonNode array) {
    List<Model> models = new ArrayList<>();
    if (array != null && array.isArray()) {
      for (JsonNode item : array) {
        models.add(JsonSupport.mapper().convertValue(item, Model.class));
      }
    }
    return models;
  }

  private static int intOr(JsonNode node, int fallback) {
    return node != null && node.isNumber() ? node.asInt() : fallback;
  }

  private static void fillEnvelope(
      com.alibaba.dashscope.types.common.ResponseBase result, ResponseEnvelope envelope) {
    result.fillEnvelope(
        JsonSupport.requestId(envelope.getBody(), envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(envelope.getBody()),
        JsonSupport.errorMessage(envelope.getBody()),
        envelope.getHeaders());
  }
}
