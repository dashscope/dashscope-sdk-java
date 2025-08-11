// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.rerank;

import com.alibaba.dashscope.api.SynchronizeHalfDuplexApi;
import com.alibaba.dashscope.common.*;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.*;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public final class TextReRank {
  
  private final SynchronizeHalfDuplexApi<TextReRankParam> syncApi;
  private final ApiServiceOption serviceOption;

  public static class Models {
    public static final String GTE_RERANK_V2 = "gte-rerank-v2";
  }

  private ApiServiceOption defaultApiServiceOption() {
    return ApiServiceOption.builder()
        .protocol(Protocol.HTTP)
        .httpMethod(HttpMethod.POST)
        .streamingMode(StreamingMode.NONE)
        .outputMode(OutputMode.ACCUMULATE)
        .taskGroup(TaskGroup.RERANK.getValue())
        .task("text-rerank")
        .function("text-rerank")
        .build();
  }

  public TextReRank() {
    serviceOption = defaultApiServiceOption();
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextReRank(String protocol) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextReRank(String protocol, String baseUrl) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(serviceOption);
  }

  public TextReRank(
      String protocol, String baseUrl, ConnectionOptions connectionOptions) {
    serviceOption = defaultApiServiceOption();
    serviceOption.setProtocol(Protocol.of(protocol));
    if (Protocol.HTTP.getValue().equals(protocol)) {
      serviceOption.setBaseHttpUrl(baseUrl);
    } else {
      serviceOption.setBaseWebSocketUrl(baseUrl);
    }
    syncApi = new SynchronizeHalfDuplexApi<>(connectionOptions, serviceOption);
  }

  /**
   * Call the server to get the whole result.
   *
   * @param param The input param of class `TextReRankParam`.
   * @return The output structure of `TextReRankResult`.
   * @throws NoApiKeyException Can not find api key
   * @throws ApiException The request failed, possibly due to a network or data error.
   */
  public TextReRankResult call(TextReRankParam param)
          throws ApiException, NoApiKeyException, InputRequiredException {
    param.validate();
    serviceOption.setIsSSE(false);
    serviceOption.setStreamingMode(StreamingMode.NONE);
    return TextReRankResult.fromDashScopeResult(syncApi.call(param));
  }
}
