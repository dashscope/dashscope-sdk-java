// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.videos;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.videos.VideoGenerateParams;

/**
 * Video generation (wan series): one-call generate (submit + wait), then the manual async-task
 * submit/wait/fetch flow, then list tasks. Translation of the Python blueprint example
 * 01_atomic_capabilities/image_video_synthesis/videos.py.
 *
 * <p>Note: video-generation tasks take a while; please be patient.
 */
public final class VideosGenerate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "wan2.7-t2v");
    String prompt = "一只小猫在月光下奔跑";

    try (DashScopeClient client = ExampleSupport.client()) {
      // One-call generate: submits the task and waits for the result
      System.out.println("----- generate (submit + wait) -----");
      Job job =
          client
              .videos()
              .generate(VideoGenerateParams.builder().model(model).prompt(prompt).build());
      if (job.isSucceeded()) {
        System.out.println("video url: " + job.getVideoUrl());
      } else {
        System.out.println("job failed: " + job.getOutput());
      }

      // Manual async-task flow: submit first, then wait/fetch by task_id
      System.out.println("----- jobs.create + wait + fetch -----");
      Job pending =
          client
              .videos()
              .jobs()
              .create(VideoGenerateParams.builder().model(model).prompt(prompt).build());
      if (pending.getOutput() == null) {
        throw new IllegalStateException("pending.getOutput() is null");
      }
      String taskId = pending.getTaskId();
      System.out.println("submitted task_id: " + taskId);

      Job finished = client.videos().jobs().wait(taskId, RequestOptions.NONE);
      System.out.println("wait finished, succeeded: " + finished.isSucceeded());
      System.out.println("video url: " + finished.getVideoUrl());

      Job snapshot = client.videos().jobs().fetch(taskId, RequestOptions.NONE);
      System.out.println("fetch output: " + snapshot.getOutput());

      // List historical tasks
      System.out.println("----- jobs.list -----");
      Page<Job> result = client.videos().jobs().list(null, RequestOptions.NONE);
      System.out.println("total tasks: " + result.getTotalCount());
      result.getItems().stream()
          .limit(3)
          .forEach(
              task ->
                  System.out.println(
                      "task_id=" + task.getTaskId() + ", status=" + task.getTaskStatus()));
    }
  }
}
