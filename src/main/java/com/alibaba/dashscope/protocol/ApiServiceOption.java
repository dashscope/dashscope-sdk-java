// Copyright (c) Alibaba, Inc. and its affiliates.

package com.alibaba.dashscope.protocol;

import com.alibaba.dashscope.common.OutputMode;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public final class ApiServiceOption implements ServiceOption {
  // set websocket service stream mode[NONE, IN, OUT, DUPLEX]
  @Default private StreamingMode streamingMode = StreamingMode.NONE;
  // set stream result output mode[accumulate, divide]
  // accumulate: Subsequent output contains previous output.
  // divide: Outputs are independent of each other.
  @Default private OutputMode outputMode = OutputMode.ACCUMULATE;
  // set communication protocol
  @Default private Protocol protocol = Protocol.HTTP;
  // if HTTP, set HTTP method.
  @Default private HttpMethod httpMethod = HttpMethod.POST;
  // Set service task group
  private String taskGroup;
  // Set service task
  private String task;
  // Set service function
  private String function;
  // Set is asynchronous task, only for HTTP
  @Default private Boolean isAsyncTask = false;
  // Set is Server-Send-Event, only for HTTP
  @Default private Boolean isSSE = false;

  @Default private Boolean isService = true;

  /** The request base url */
  @Default private String baseHttpUrl = null;

  @Default private String baseWebSocketUrl = null;
  @Default private boolean passTaskStarted = false;

  @Override
  public String httpUrl() {
    StringBuffer sb = new StringBuffer();
    if (isService) {
      sb.append("/services");
    }
    if (taskGroup != null) {
      sb.append("/");
      sb.append(taskGroup);
    }
    if (task != null) {
      sb.append("/");
      sb.append(task);
    }
    if (function != null) {
      sb.append("/");
      sb.append(function);
    }
    return sb.toString();
  }
}
