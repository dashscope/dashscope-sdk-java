// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.images;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.common.Page;
import com.alibaba.dashscope.types.images.ImageGenerateParams;

/**
 * Image generation (wanx series): one-call generate (submit + wait), then the manual async-task
 * submit/wait/fetch flow, then list tasks. Translation of the Python blueprint example
 * 01_atomic_capabilities/image_video_synthesis/images.py.
 */
public final class ImagesGenerate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "wanx2.1-t2i-turbo");
    String prompt = "一间有着精致窗户的花店，漂亮的木质门，摆放着花朵";

    try (DashScopeClient client = ExampleSupport.client()) {
      // One-call generate: submits the task and waits for the result
      System.out.println("----- generate (submit + wait) -----");
      Job job =
          client
              .images()
              .generate(
                  ImageGenerateParams.builder()
                      .model(model)
                      .prompt(prompt)
                      .n(1)
                      .size("1024*1024")
                      .build());
      if (job.isSucceeded()) {
        for (String url : job.getImageUrls()) {
          System.out.println("image url: " + url);
        }
      } else {
        System.out.println("job failed: " + job.getOutput());
      }

      // Manual async-task flow: submit first, then wait/fetch by task_id
      System.out.println("----- jobs.create + wait + fetch -----");
      Job pending =
          client
              .images()
              .jobs()
              .create(
                  ImageGenerateParams.builder()
                      .model(model)
                      .prompt(prompt)
                      .n(1)
                      .size("1024*1024")
                      .build());
      if (pending.getOutput() == null) {
        throw new IllegalStateException("pending.getOutput() is null");
      }
      String taskId = pending.getTaskId();
      System.out.println("submitted task_id: " + taskId);

      Job finished = client.images().jobs().wait(taskId, RequestOptions.NONE);
      System.out.println("wait finished, succeeded: " + finished.isSucceeded());

      Job snapshot = client.images().jobs().fetch(taskId, RequestOptions.NONE);
      System.out.println("fetch output: " + snapshot.getOutput());

      // List historical tasks
      System.out.println("----- jobs.list -----");
      Page<Job> result = client.images().jobs().list(null, RequestOptions.NONE);
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
