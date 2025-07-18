// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.audio.omni;

import static com.alibaba.dashscope.utils.Constants.DASHSCOPE_WEBSOCKET_OMNI_BASE_URL_ENV;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

/** @author lengjiayi */
@Data
@SuperBuilder
public class OmniRealtimeParam {

  public static String baseWebsocketApiUrl =
      System.getenv()
          .getOrDefault(
              DASHSCOPE_WEBSOCKET_OMNI_BASE_URL_ENV,
              String.format("wss://dashscope.aliyuncs.com/api-ws/v1/realtime"));

  /** The model to use. */
  @Getter @lombok.NonNull private String model;

  /** The apikey. */
  @Getter private String apikey;

  @Getter private String workspace;

  /** The custom http header. */
  @Singular protected Map<String, Object> headers;

  private String url;

  public Map<String, String> getHeaders() {
    Map<String, String> res = new HashMap<>();
    for (Map.Entry<String, Object> entry : headers.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue().toString();
      res.put(key, value);
    }
    return res;
  }

  public String getUrl() {
    if (url != null) {
      return url + "?model=" + model;
    } else {
      return baseWebsocketApiUrl + "?model=" + model;
    }
  }
}
