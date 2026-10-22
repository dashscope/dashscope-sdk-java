// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.examples.atomic.multimodal;

import com.alibaba.dashscope.DashScopeClient;
import com.alibaba.dashscope.examples.ExampleSupport;
import com.alibaba.dashscope.v3.types.common.ResponseStream;
import com.alibaba.dashscope.v3.types.multimodal.ContentPart;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatChunk;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatCompletion;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChatCreateParams;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalChoice;
import com.alibaba.dashscope.v3.types.multimodal.MultimodalMessage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Multimodal chat (qwen-vl image understanding): non-streaming and streaming. Translation of the
 * Python blueprint example 01_atomic_capabilities/multimodal_vision/multimodal_chat.py.
 */
public final class MultimodalChatCreate {

  public static void main(String[] args) throws Exception {
    String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen3-vl-plus");
    // Replace with your own image URL (must be publicly accessible)
    String imageUrl =
        "https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/"
            + "zh-CN/20241022/emyrja/dog_and_girl.jpeg";
    List<MultimodalMessage> messages =
        Collections.singletonList(
            MultimodalMessage.user(
                Arrays.asList(
                    ContentPart.image(imageUrl), ContentPart.text("图中描绘的是什么景象?"))));

    try (DashScopeClient client = ExampleSupport.client()) {
      // Non-streaming
      System.out.println("----- non-streaming request -----");
      MultimodalChatCompletion resp =
          client
              .multimodal()
              .chat()
              .create(
                  MultimodalChatCreateParams.builder()
                      .model(model)
                      .messages(messages)
                      .build());
      System.out.println(resp.outputText());
      System.out.println(
          "usage: "
              + resp.getUsage().getTotalTokens()
              + " tokens, request_id: "
              + resp.getRequestId());

      // Streaming
      System.out.println("----- streaming request -----");
      try (ResponseStream<MultimodalChatChunk> stream =
          client
              .multimodal()
              .chat()
              .createStreaming(
                  MultimodalChatCreateParams.builder()
                      .model(model)
                      .messages(messages)
                      .build())) {
        for (MultimodalChatChunk chunk : stream) {
          MultimodalChoice choice = chunk.firstChoice();
          if (choice == null || choice.getMessage() == null) {
            continue;
          }
          String text = choice.getMessage().text();
          if (!text.isEmpty()) {
            System.out.print(text);
            System.out.flush();
          }
        }
      }
      System.out.println();
    }
  }
}
