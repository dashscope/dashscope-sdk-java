// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.audio;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.RequestOptions;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.resources.jobs.Job;
import com.alibaba.dashscope.types.tingwu.TingWuJobParams;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Offline TingWu job: create a task whose free-form input carries the business-defined fields
 * ({@code fileUrl} — the audio/video file to analyze — and {@code appid} — the TingWu app ID), wait
 * for the terminal state, and print the result URL. Replace the appid default and pass your own
 * publicly accessible file URL before running. Only the offline-jobs part of the Python original is
 * covered here (its realtime websocket session is a separate capability). Usage:
 *
 * <pre>
 * TingWuJobsCreate --url &lt;audio/video file url&gt; [--app-id 123456] [--model tingwu-automotive-service-inspection]
 * </pre>
 *
 * Translation of the Python blueprint example 01_atomic_capabilities/audio/tingwu.py.
 */
public final class TingWuJobsCreate {

  public static void main(String[] args) throws Exception {
    String url = null;
    String appId = "123456"; // Placeholder, as in the Python original; use your own TingWu app ID.
    String model = "tingwu-automotive-service-inspection";
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--url":
          url = value(args, ++i);
          break;
        case "--app-id":
          appId = value(args, ++i);
          break;
        case "--model":
          model = value(args, ++i);
          break;
        default:
          usage();
          System.exit(2);
      }
    }
    if (url == null) {
      usage();
      System.exit(2);
    }

    Map<String, Object> input = new LinkedHashMap<>();
    input.put("fileUrl", url);
    input.put("appid", appId);
    try (DashScopeClient client = ExampleSupport.client()) {
      Job created =
          client
              .tingwu()
              .jobs()
              .create(TingWuJobParams.builder().model(model).input(input).build());
      if (created.isError()) {
        System.err.println("error: " + created.getCode() + " " + created.getMessage());
        System.exit(1);
      }
      System.out.println("task_id: " + created.getTaskId());
      Job job = client.tingwu().jobs().wait(created.getTaskId(), RequestOptions.NONE);
      System.out.println(
          "Task status: " + job.getTaskStatus() + " request_id=" + job.getRequestId());
      if (job.getResultUrl() != null) {
        System.out.println("result_url: " + job.getResultUrl());
      }
      System.out.println(job.getOutput());
      if (!job.isSucceeded()) {
        System.exit(1);
      }
    }
  }

  private static String value(String[] args, int i) {
    if (i >= args.length) {
      usage();
      System.exit(2);
    }
    return args[i];
  }

  private static void usage() {
    System.err.println(
        "usage: TingWuJobsCreate --url <audio/video file url> [--app-id ID] [--model M]");
  }
}
