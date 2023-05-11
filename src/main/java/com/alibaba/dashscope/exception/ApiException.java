package com.alibaba.dashscope.exception;

import com.alibaba.dashscope.common.Status;
import com.alibaba.dashscope.utils.JsonUtils;
import lombok.Getter;

public class ApiException extends RuntimeException {

  @Getter private Status status = null;

  public ApiException(Exception e) {
    super(e);
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
