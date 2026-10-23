// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.v3.resources.videos;

import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.v3.resources.jobs.Job;
import com.alibaba.dashscope.v3.transport.HttpTransport;
import com.alibaba.dashscope.v3.types.videos.VideoGenerateParams;

/**
 * {@code videos} resource (design §3): {@code generate} is the blocking sugar — submit the task and
 * wait for its terminal state; the finished {@link Job}'s {@code getVideoUrl()} reads the same for
 * both input protocols.
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

  /**
   * Blocks until a terminal state; equivalent to jobs().create + jobs().wait.
   *
   * @see #generate(VideoGenerateParams, RequestOptions)
   */
  public Job generate(VideoGenerateParams params) {
    return generate(params, RequestOptions.NONE);
  }

  /** Blocks until a terminal state; equivalent to jobs().create + jobs().wait. */
  public Job generate(VideoGenerateParams params, RequestOptions options) {
    Job created = jobs.create(params, options);
    return jobs.wait(created.getTaskId(), options);
  }
}
