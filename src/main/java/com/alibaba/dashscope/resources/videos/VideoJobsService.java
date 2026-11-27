// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.videos;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.resources.jobs.JobsService;
import com.alibaba.dashscope.resources.oss.OssMediaUpload;
import com.alibaba.dashscope.resources.oss.OssUploader;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.videos.VideoGenerateParams;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Video async-task facade (design §3): create/fetch/wait/cancel/list over the video service. */
public final class VideoJobsService {
  private final JobsService jobs;
  private final OssUploader uploader;

  VideoJobsService(HttpTransport transport) {
    this.jobs = new JobsService(transport, VideosSupport.MODULE);
    this.uploader = new OssUploader(transport);
  }

  public Job create(VideoGenerateParams params) {
    return create(params, RequestOptions.NONE);
  }

  public Job create(VideoGenerateParams params, RequestOptions options) {
    OssMediaUpload upload = new OssMediaUpload(uploader, params.getModel(), options);
    String imageUrl =
        params.getImageUrl() == null ? null : (String) upload.process(params.getImageUrl());
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> media =
        params.getMedia() == null
            ? null
            : (List<Map<String, Object>>) upload.process(params.getMedia());
    @SuppressWarnings("unchecked")
    Map<String, Object> extraInput =
        params.getExtraInput() == null
            ? null
            : (Map<String, Object>) upload.process(params.getExtraInput());
    Map<String, String> headers =
        upload.isOssResolve()
            ? Collections.singletonMap(OssMediaUpload.RESOLVE_HEADER, OssMediaUpload.RESOLVE_ENABLE)
            : null;
    ObjectNode body = VideosSupport.buildBody(params, imageUrl, media, extraInput);
    return jobs.create(VideosSupport.PATH, body, headers, options);
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
}
