// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.images;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.resources.oss.OssMediaUpload;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.transport.JsonSupport;
import com.alibaba.dashscope.v3.transport.ResponseEnvelope;
import com.alibaba.dashscope.v3.types.images.ImageGenerateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;

/**
 * {@code images} resource (design §3): {@code generate} is the blocking sugar — legacy models
 * submit an async task and wait; multimodal-protocol models (wan2.6-t2i / wan2.6-image) answer
 * synchronously in one request. Both return the unified {@link Job} — {@code getImageUrls()} reads
 * the same for both output shapes.
 */
public final class ImagesService {
  private final ImageJobsService jobs;

  public ImagesService(HttpTransport transport) {
    this.jobs = new ImageJobsService(transport);
  }

  /** The async-job handle: create/fetch/wait/cancel/list. */
  public ImageJobsService jobs() {
    return jobs;
  }

  /**
   * Blocks until a terminal state; equivalent to jobs().create + jobs().wait (multimodal-protocol
   * models answer in one synchronous request).
   *
   * @see #generate(ImageGenerateParams, RequestOptions)
   */
  public Job generate(ImageGenerateParams params) {
    return generate(params, RequestOptions.NONE);
  }

  /**
   * Blocks until a terminal state; equivalent to jobs().create + jobs().wait (multimodal-protocol
   * models answer in one synchronous request).
   */
  public Job generate(ImageGenerateParams params, RequestOptions options) {
    if (!ImageGenerateParams.usesMultimodalProtocol(params.getModel())) {
      Job created = jobs.create(params, options);
      return jobs.wait(created.getTaskId(), options);
    }
    // Multimodal protocol: one-shot sync call wrapped in the unified Job contract.
    OssMediaUpload upload = new OssMediaUpload(jobs.uploader(), params.getModel(), options);
    @SuppressWarnings("unchecked")
    List<String> imageUrls =
        params.getImageUrls() == null ? null : (List<String>) upload.process(params.getImageUrls());
    @SuppressWarnings("unchecked")
    Map<String, Object> extraInput =
        params.getExtraInput() == null
            ? null
            : (Map<String, Object>) upload.process(params.getExtraInput());
    ObjectNode body = ImagesSupport.buildMultimodalBody(params, imageUrls, extraInput);
    ResponseEnvelope envelope =
        jobs.transport()
            .execute(
                ImagesSupport.MULTIMODAL_SYNC_PATH,
                body,
                ImageJobsService.resolveHeaders(upload),
                ImagesSupport.MODULE,
                options);
    return syncJob(envelope);
  }

  /** Wrap a one-shot multimodal answer as a finished Job (no task fields on the wire). */
  private static Job syncJob(ResponseEnvelope envelope) {
    JsonNode body = envelope.getBody();
    if (body != null && body.isObject() && !body.hasNonNull("code")) {
      JsonNode output = body.get("output");
      if (output != null && output.isObject() && !output.hasNonNull("task_status")) {
        ((ObjectNode) output).put("task_status", Job.SUCCEEDED);
      }
    }
    Job job = Job.fromBody(body);
    job.fillEnvelope(
        JsonSupport.requestId(body, envelope.getHeaders()),
        envelope.getStatusCode(),
        JsonSupport.errorCode(body),
        JsonSupport.errorMessage(body),
        envelope.getHeaders());
    return job;
  }
}
