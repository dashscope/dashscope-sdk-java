package com.dashscope.exception;

import com.alibaba.fastjson.JSON;
import com.dashscope.common.Status;
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
    s = s + "; status body:" + JSON.toJSONString(status);
    return s;
  }
}
