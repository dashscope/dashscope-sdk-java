// Copyright (c) Alibaba, Inc. and its affiliates.
package com.alibaba.dashscope.resources.agentstudio.support.utils;

/** 3.0 copy of the 2.x StringUtils surface used by agentstudio: {@link #format} only. */
public final class StringUtils {
  private StringUtils() {}

  public static String format(String format, Object... args) {
    return String.format(format, args);
  }
}
