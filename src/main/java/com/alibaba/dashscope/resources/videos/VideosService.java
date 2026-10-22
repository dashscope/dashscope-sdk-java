// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.videos;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.transport.HttpTransport;
import com.alibaba.dashscope.types.videos.VideoGenerateParams;

/**
 * {@code videos} resource (design §3): {@code generate} is the blocking sugar — submit the task
 * and wait for its terminal state; the finished {@link Job}'s {@code getVideoUrl()} reads the
 * same for both input protocols.
 */
public final class VideosService {
  private final VideoJobsService jobs;

  public VideosService(HttpTransport transport) {
    this.jobs = new VideoJobsService(transport);
  }

  /** The async-job handle: create/fetch/wait/cancel/list. */
  public VideoJobsService jobs() {
    return jobs;
  }

  public Job generate(VideoGenerateParams params) {
    return generate(params, RequestOptions.NONE);
  }

  public Job generate(VideoGenerateParams params, RequestOptions options) {
    Job created = jobs.create(params, options);
    return jobs.wait(created.getTaskId(), options);
  }
}
