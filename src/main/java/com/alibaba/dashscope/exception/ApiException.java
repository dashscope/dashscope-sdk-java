// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.exception;

import com.alibaba.dashscope.common.ErrorType;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpStatus;

public class ApiException extends RuntimeException {

  @Getter @Setter private Status status = null;

  public ApiException(Throwable e) {
    super(e);
    this.status =
        Status.builder()
            .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .code(ErrorType.UNKNOWN_ERROR.getValue())
            .message(e.getMessage())
            .build();
    this.setStackTrace(e.getStackTrace());
  }

  public ApiException(Status status) {
    super();
    this.status = status;
  }

  @Override
  public String toString() {
    String s = super.toString();
    s = s + "; status body:" + JsonUtils.toJson(status);
    return s;
  }
}
