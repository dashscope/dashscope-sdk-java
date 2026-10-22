// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.images;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobsService;
import com.alibaba.dashscope.resources.oss.OssMediaUpload;
import com.alibaba.dashscope.resources.oss.OssUploader;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.images.ImageGenerateParams;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Image async-task facade (design §3): {@code create} submits legacy models to the text2image
 * endpoint and multimodal-protocol models to the image-generation task endpoint; {@code fetch} /
 * {@code wait} / {@code cancel} / {@code list} poll {@code /tasks} either way.
 */
public final class ImageJobsService {
  private final HttpTransport transport;
  private final OssUploader uploader;
  private final JobsService jobs;

  ImageJobsService(HttpTransport transport) {
    this.transport = transport;
    this.uploader = new OssUploader(transport);
    this.jobs = new JobsService(transport, ImagesSupport.MODULE);
  }

  public Job create(ImageGenerateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Job create(ImageGenerateParams params, RequestOptions options) {
    OssMediaUpload upload = new OssMediaUpload(uploader, params.getModel(), options);
    @SuppressWarnings("unchecked")
    List<String> imageUrls =
        params.getImageUrls() == null
            ? null
            : (List<String>) upload.process(params.getImageUrls());
    @SuppressWarnings("unchecked")
    Map<String, Object> extraInput =
        params.getExtraInput() == null
            ? null
            : (Map<String, Object>) upload.process(params.getExtraInput());
    Map<String, String> headers = resolveHeaders(upload);

    ObjectNode body =
        ImageGenerateParams.usesMultimodalProtocol(params.getModel())
            ? ImagesSupport.buildMultimodalBody(params, imageUrls, extraInput)
            : ImagesSupport.buildLegacyBody(params, imageUrls, extraInput);
    return jobs.create(ImagesSupport.taskPath(params.getModel()), body, headers, options);
  }

  public Job fetch(String taskId, RequestOptions options) {
    return jobs.fetch(taskId, options);
  }

  public Job wait(String taskId, RequestOptions options) {
    return jobs.wait(taskId, options);
  }

  public Job cancel(String taskId, RequestOptions options) {
    return jobs.cancel(taskId, options);
  }

  public Page<Job> list(Map<String, String> query, RequestOptions options) {
    return jobs.list(query, options);
  }

  static Map<String, String> resolveHeaders(OssMediaUpload upload) {
    return upload.isOssResolve()
        ? Collections.singletonMap(OssMediaUpload.RESOLVE_HEADER, OssMediaUpload.RESOLVE_ENABLE)
        : null;
  }

  HttpTransport transport() {
    return transport;
  }

  OssUploader uploader() {
    return uploader;
  }
}
