// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.deployments;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.types.common.JobStatus;
import com.alibaba.dashscope.v3.types.common.ResponseBase;
import com.alibaba.dashscope.v3.types.deployments.Deployment;
import com.alibaba.dashscope.v3.types.deployments.DeploymentCreateParams;
import com.alibaba.dashscope.v3.types.deployments.DeploymentList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code deployments} resource (design §3, Python v2 deployments.py parity): create/get/list/
 * scale/delete over {@code /deployments} plus the {@code streamEvents} SSE feed. Module tag
 * follows the 1.x lineage (Deployments lived in dashscope.finetune).
 */
public final class DeploymentsService {
  static final String BASE_PATH = "/deployments";
  static final String MODULE = "finetune";

  private final HttpTransport transport;

  public DeploymentsService(HttpTransport transport) {
    this.transport = transport;
  }

  public Deployment create(DeploymentCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Deployment create(DeploymentCreateParams params, RequestOptions options) {
    ResponseEnvelope envelope = transport.execute(BASE_PATH, buildBody(params), MODULE, options);
    return toDeployment(envelope);
  }

  public Deployment get(String deployedModel) {
    return get(deployedModel, RequestOptions.NONE);
  }

  public Deployment get(String deployedModel, RequestOptions options) {
    requireDeployedModel(deployedModel);
    ResponseEnvelope envelope =
        transport.executeGet(BASE_PATH + "/" + deployedModel, null, MODULE, options);
    return toDeployment(envelope);
  }

  public DeploymentList list(int pageNo, int pageSize) {
    return list(pageNo, pageSize, RequestOptions.NONE);
  }

  public DeploymentList list(int pageNo, int pageSize, RequestOptions options) {
    Map<String, String> query = new LinkedHashMap<>();
    query.put("page_no", String.valueOf(pageNo));
    query.put("page_size", String.valueOf(pageSize));
    ResponseEnvelope envelope = transport.executeGet(BASE_PATH, query, MODULE, options);
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    List<Deployment> deployments = new ArrayList<>();
    JsonNode items = flat.get("deployments");
    if (items != null && items.isArray()) {
      for (JsonNode item : items) {
        deployments.add(JsonSupport.mapper().convertValue(item, Deployment.class));
      }
    }
    DeploymentList result =
        new DeploymentList(
            intOr(flat.get("page_no"), 1),
            intOr(flat.get("page_size"), 10),
            intOr(flat.get("total"), deployments.size()),
            deployments);
    fillFromFlat(result, flat, envelope);
    return result;
  }

  public Deployment scale(String deployedModel, int capacity) {
    return scale(deployedModel, capacity, RequestOptions.NONE);
  }

  public Deployment scale(String deployedModel, int capacity, RequestOptions options) {
    requireDeployedModel(deployedModel);
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode body = mapper.createObjectNode();
    body.put("deployed_model", deployedModel);
    body.put("capacity", capacity);
    ResponseEnvelope envelope =
        transport.executePut(BASE_PATH + "/" + deployedModel + "/scale", body, MODULE, options);
    return toDeployment(envelope);
  }

  public JobStatus delete(String deployedModel) {
    return delete(deployedModel, RequestOptions.NONE);
  }

  public JobStatus delete(String deployedModel, RequestOptions options) {
    requireDeployedModel(deployedModel);
    ResponseEnvelope envelope =
        transport.executeDelete(BASE_PATH + "/" + deployedModel, MODULE, options);
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    JobStatus status = JsonSupport.mapper().convertValue(flat, JobStatus.class);
    fillFromFlat(status, flat, envelope);
    return status;
  }

  /** The deployment-event SSE feed. Close the returned stream (try-with-resources). */
  public DeploymentEventStream streamEvents(String deployedModel) {
    return streamEvents(deployedModel, RequestOptions.NONE);
  }

  public DeploymentEventStream streamEvents(String deployedModel, RequestOptions options) {
    requireDeployedModel(deployedModel);
    return new DeploymentEventStream(
        transport.executeJobEventStream(
            BASE_PATH + "/" + deployedModel + "/stream", MODULE, options),
        transport.config().isRaiseOnError());
  }

  private static ObjectNode buildBody(DeploymentCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode body = mapper.createObjectNode();
    // Wire spellings; the public names are model / modelVersion.
    body.put("model_name", params.getModel());
    body.put("capacity", params.getCapacity());
    if (params.getModelVersion() != null) {
      body.put("model_version", params.getModelVersion());
    }
    if (params.getSuffix() != null) {
      body.put("suffix", params.getSuffix());
    }
    if (params.getPlan() != null) {
      body.put("plan", params.getPlan());
    }
    if (params.getTemplateId() != null) {
      body.put("template_id", params.getTemplateId());
    }
    if (params.getDeploySpec() != null) {
      body.set("deploy_spec", mapper.valueToTree(params.getDeploySpec()));
    }
    if (params.getBillingMethod() != null) {
      body.put("billing_method", params.getBillingMethod());
    }
    return body;
  }

  private static Deployment toDeployment(ResponseEnvelope envelope) {
    ObjectNode flat = JsonSupport.flattenEnvelope(envelope.getBody());
    Deployment deployment = JsonSupport.mapper().convertValue(flat, Deployment.class);
    fillFromFlat(deployment, flat, envelope);
    return deployment;
  }

  private static void fillFromFlat(
      ResponseBase result, ObjectNode flat, ResponseEnvelope envelope) {
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
  }

  private static void requireDeployedModel(String deployedModel) {
    if (deployedModel == null || deployedModel.isEmpty()) {
      throw new IllegalArgumentException("deployed_model is required");
    }
  }

  private static int intOr(JsonNode node, int fallback) {
    return node != null && node.isNumber() ? node.asInt() : fallback;
  }

  private static String textOrNull(JsonNode node) {
    return node != null && node.isTextual() ? node.asText() : null;
  }
}
