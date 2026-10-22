// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.exception;

import com.alibaba.dashscope.resources.agentstudio.support.common.Status;
import com.alibaba.dashscope.resources.agentstudio.support.utils.JsonUtils;
import com.alibaba.dashscope.resources.agentstudio.support.utils.StringUtils;
import lombok.Getter;
import lombok.Setter;

/**
 * 3.0 copy of the 2.x-style ApiException for the agentstudio subtree (design §7: the subarea keeps
 * its own exception style until a later 3.x aligns it). The 2.x shim layer translates this back to
 * {@code com.alibaba.dashscope.exception.ApiException}; the 3.0 mount translates it into the 3.0
 * error family.
 */
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
              .code("network error")
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
