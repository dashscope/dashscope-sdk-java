// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.tingwu;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobsService;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.transport.JsonSupport;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.tingwu.TingWuJobParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;

/**
 * Offline TingWu summarization as a long task (design §3 resource tree): the submit goes to the
 * multimodal-generation service with the async header; poll with {@code fetch}/{@code wait}.
 */
public final class TingWuJobsService {
  private final JobsService jobs;

  TingWuJobsService(HttpTransport transport) {
    this.jobs = new JobsService(transport, TingWuService.MODULE);
  }

  public Job create(TingWuJobParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Job create(TingWuJobParams params, RequestOptions options) {
    return jobs.create(TingWuService.PATH, buildBody(params), options);
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

  static ObjectNode buildBody(TingWuJobParams params) {
    ObjectMapper mapper = JsonSupport.mapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("model", params.getModel());
    ObjectNode input = root.putObject("input");
    params.getInput().forEach((k, v) -> input.set(k, mapper.valueToTree(v)));
    if (params.getParameters() != null && !params.getParameters().isEmpty()) {
      ObjectNode parameters = root.putObject("parameters");
      params.getParameters().forEach((k, v) -> parameters.set(k, mapper.valueToTree(v)));
    }
    return root;
  }
}
