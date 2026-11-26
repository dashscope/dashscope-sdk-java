// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.protocol;

import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/** Service options for the AgentStudio HTTP transport. */
@Data
@SuperBuilder
public final class GeneralServiceOption implements ServiceOption {
  // set service stream mode[NONE, IN, OUT, DUPLEX]
  @Default private StreamingMode streamingMode = StreamingMode.NONE;
  // set communication protocol
  @Default private Protocol protocol = Protocol.HTTP;
  // if HTTP, set HTTP method.
  @Default private HttpMethod httpMethod = HttpMethod.POST;
  // Set service path
  private String path;
  // Set is asynchronous task, only for HTTP
  @Default private Boolean isAsyncTask = false;
  // Set is Server-Send-Event, only for HTTP
  @Default private Boolean isSSE = false;

  @Default private Boolean isService = false;

  /** The request base url */
  @Default private String baseHttpUrl = null;

  /** SDK module name for tracking headers (e.g. "agentstudio3"). */
  @Default private String module = null;

  @Override
  public String httpUrl() {
    StringBuffer sb = new StringBuffer();
    if (isService) {
      sb.append("/services");
    }
    if (path != null) {
      sb.append("/");
      sb.append(path);
    }
    return sb.toString();
  }

  @Override
  public boolean getIsFlatten() {
    return true;
  }

  @Override
  public String getModule() {
    return module;
  }
}
