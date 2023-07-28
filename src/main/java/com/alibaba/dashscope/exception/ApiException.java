// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.exception;

import com.alibaba.dashscope.common.ErrorType;
import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.utils.JsonUtils;
import java.net.HttpURLConnection;
import lombok.Getter;
import lombok.Setter;

public class ApiException extends RuntimeException {

  @Getter @Setter private Status status = null;

  public ApiException(Throwable e) {
    super(e);
    if (e instanceof ApiException) {
      this.status = ((ApiException) e).status;
    } else {
      this.status =
          Status.builder()
              .statusCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
              .code(ErrorType.UNKNOWN_ERROR.getValue())
              .message(e.getMessage())
              .build();
    }
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

  @Override
  public String getMessage() {
    return JsonUtils.toJson(status);
  }
}
