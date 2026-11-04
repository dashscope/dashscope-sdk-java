// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.legacy_2x.exception;

import com.alibaba.dashscope.legacy_2x.common.ErrorType;
import com.alibaba.dashscope.legacy_2x.common.Status;
import com.alibaba.dashscope.legacy_2x.utils.JsonUtils;
import com.alibaba.dashscope.legacy_2x.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

@Deprecated
public class ApiException extends RuntimeException {

  @Getter @Setter private Status status = null;

  public ApiException(Throwable e) {
    super(e);
    if (e instanceof ApiException) {
      this.status = ((ApiException) e).status;
    } else {
      this.status =
          Status.builder()
              .statusCode(-1)
              .code(ErrorType.NETWORK_ERROR.getValue())
              .message(StringUtils.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()))
              .build();
    }
    this.setStackTrace(e.getStackTrace());
  }

  public ApiException(Status status) {
    super();
    this.status = status;
  }

  public ApiException(Status status, Throwable th) {
    super(status.getMessage(), th);
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
