// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.embeddings;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobsService;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.embeddings.BatchEmbeddingCreateParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

/**
 * Async batch-embedding task facade (design §3, Python v2 EmbeddingJobs parity): the submit goes to
 * the same service path as the sync endpoint with the async header and a file-url input; the
 * finished {@link Job}'s {@code getResultUrl()} carries the output file.
 */
public final class EmbeddingJobsService {
  private final JobsService jobs;

  EmbeddingJobsService(HttpTransport transport) {
    this.jobs = new JobsService(transport, EmbeddingService.MODULE);
  }

  public Job create(BatchEmbeddingCreateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Job create(BatchEmbeddingCreateParams params, RequestOptions options) {
    return jobs.create(EmbeddingService.PATH, buildBody(params), options);
  }

  public Job fetch(String taskId, RequestOptions options) {
    return jobs.fetch(taskId, options);
  }

  public Job wait(String taskId, RequestOptions options) {
    return jobs.wait(taskId, options);
  }

  /** Cancel a PENDING task ({@code POST /tasks/{task_id}/cancel}). */
  public Job cancel(String taskId, RequestOptions options) {
    return jobs.cancel(taskId, options);
  }

  /** List async tasks ({@code GET /tasks}); query keys follow the wire names. */
  public Page<Job> list(Map<String, String> query, RequestOptions options) {
    return jobs.list(query, options);
  }

  static ObjectNode buildBody(BatchEmbeddingCreateParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    root.putObject("input").put("url", params.getFileUrl());
    ObjectNode parameters = mapper.createObjectNode();
    if (params.getTextType() != null) {
      parameters.put("text_type", params.getTextType());
    }
    if (params.getParameters() != null) {
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    if (!parameters.isEmpty()) {
      root.set("parameters", parameters);
    }
    return root;
  }
}
