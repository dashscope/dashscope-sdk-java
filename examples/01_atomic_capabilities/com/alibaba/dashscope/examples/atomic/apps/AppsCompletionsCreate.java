// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.apps;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.types.apps.AppCompletion;
import com.alibaba.dashscope.types.apps.AppCompletionCreateParams;
import com.alibaba.dashscope.types.common.ResponseStream;

/**
 * Bailian application call: non-streaming and streaming. Translation of the Python blueprint
 * example 01_atomic_capabilities/applications/apps.py.
 *
 * <p>APP_ID: the application ID of a Model Studio app; provide it via the {@code DASHSCOPE_APP_ID}
 * environment variable.
 */
public final class AppsCompletionsCreate {

  public static void main(String[] args) throws Exception {
    String appId = System.getenv().getOrDefault("DASHSCOPE_APP_ID", "<YOUR_APP_ID>");
    String prompt = "如何做土豆炖猪脚?";

    try (DashScopeClient client = ExampleSupport.client()) {
      // Non-streaming
      System.out.println("----- non-streaming request -----");
      AppCompletion resp =
          client
              .apps()
              .completions()
              .create(AppCompletionCreateParams.builder().appId(appId).prompt(prompt).build());
      System.out.println(resp.getText());
      System.out.println(
          "session_id: " + resp.getSessionId() + ", request_id: " + resp.getRequestId());

      // Streaming: each frame carries the completion text as it arrives
      System.out.println("----- streaming request -----");
      try (ResponseStream<AppCompletion> stream =
          client
              .apps()
              .completions()
              .createStreaming(
                  AppCompletionCreateParams.builder().appId(appId).prompt(prompt).build())) {
        for (AppCompletion chunk : stream) {
          if (chunk.getText() != null && !chunk.getText().isEmpty()) {
            System.out.print(chunk.getText());
            System.out.flush();
          }
        }
      }
      System.out.println();
    }
  }
}
