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
  @lombok.NonNull private String taskGroup;
  // Set service task
  @lombok.NonNull private String task;
  // Set service function
  @lombok.NonNull private String function;
  // Set is asynchronous task, only for HTTP
  @Default private Boolean isAsyncTask = false;
  // Set is Server-Send-Event, only for HTTP
  @Default private Boolean isSSE = false;

  @Override
  public String httpUrl() {
    return String.format("/services/%s/%s/%s", taskGroup, task, function);
  }
}
