// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import java.util.Map;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/** Dashscope Http request */
@Data
@SuperBuilder
public class HttpRequest {
  private HttpMethod httpMethod;
  private String url;
  private Map<String, String> headers;
  private Map<String, Object> parameters;
  private Object body;
}
