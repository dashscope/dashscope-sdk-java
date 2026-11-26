// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.applications.agentstudio.exception;

/** Missing required input fields. */
public class InputRequiredException extends Exception {
  public InputRequiredException(String msg) {
    super(msg);
  }
}
