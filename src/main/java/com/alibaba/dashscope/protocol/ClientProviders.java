// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.protocol.okhttp.OkHttpClientFactory;
import com.alibaba.dashscope.protocol.okhttp.OkHttpHttpClient;
import com.alibaba.dashscope.protocol.okhttp.OkHttpWebSocketClient;

public class ClientProviders {
  public static HalfDuplexClient getHalfDuplexClient(String protocol) {
    return getHalfDuplexClient(null, protocol);
  }

  public static FullDuplexClient getFullDuplexClient() {
    return getFullDuplexClient(null);
  }

  /**
   * Create a dashscope half duplex client. only okhttp http and websocket is supported.
   *
   * @param options The client connection options.
   * @param protocol The protocol to use, one of "http|https|websocket"
   * @return The half duplex client.
   */
  public static HalfDuplexClient getHalfDuplexClient(ConnectionOptions options, String protocol) {
    if (protocol == null) {
      protocol = "https";
    }
    if (options == null) {
      // create default config client, create default http client.
      if (protocol.toLowerCase().startsWith("http")) {
        return new OkHttpHttpClient(OkHttpClientFactory.getOkHttpClient());
      } else {
        return new OkHttpWebSocketClient(OkHttpClientFactory.getOkHttpClient());
      }
    } else {
      if (protocol.toLowerCase().startsWith("http")) {
        return new OkHttpHttpClient(OkHttpClientFactory.getNewOkHttpClient(options));
      } else {
        return new OkHttpWebSocketClient(OkHttpClientFactory.getNewOkHttpClient(options));
      }
    }
  }

  /**
   * Create a dashscope full duplex client. only websocket is supported.
   *
   * @param connectionOptions The client options.
   * @return The full duplex client.
   */
  public static FullDuplexClient getFullDuplexClient(ConnectionOptions connectionOptions) {
    if (connectionOptions == null) {
      // create default config client, create default http client.
      return new OkHttpWebSocketClient(OkHttpClientFactory.getOkHttpClient());
    } else {
      return new OkHttpWebSocketClient(OkHttpClientFactory.getNewOkHttpClient(connectionOptions));
    }
  }
}
